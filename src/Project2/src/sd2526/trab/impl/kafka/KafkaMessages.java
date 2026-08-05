package sd2526.trab.impl.kafka;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.db.DB;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.java.servers.InboxEntry;
import sd2526.trab.impl.java.servers.JavaBaseService;


public class KafkaMessages extends JavaBaseService implements Messages, AdminMessages {

    static final Logger Log = Logger.getLogger(KafkaMessages.class.getName());
    static final Gson GSON = new Gson();

    static final String KAFKA_ADDR = System.getProperty("kafka.addr", "kafka:9092");
    static final long MSG_CACHE_TTL = 30_000;
    static final long GC_CACHE_TTL = 10_000;
    static final int REMOTE_DEADLINE = 90_000;

    static final String OP_DELIVER = "DELIVER";
    static final String OP_REMOTE_DELIVER = "REMOTE_DELIVER";
    static final String OP_DELETE_INBOX = "DELETE_INBOX";
    static final String OP_REMOVE_ENTRY = "REMOVE_ENTRY";
    static final String OP_DELETE_USER = "DELETE_USER";

    static class KafkaEvent {
        String type;
        Message msg;
        List<String> recipients = new ArrayList<>();
        List<String> localUnknown = new ArrayList<>();
        List<String> remoteUnknown = new ArrayList<>();
        String originatorUri;
        String mid;
        String name;
        String pwd;
    }


    private static KafkaMessages instance;

    public static synchronized KafkaMessages getInstance() {
        if (instance == null)
            instance = new KafkaMessages(computeMyUri());
        return instance;
    }

    private static String computeMyUri() {
        try {
            return "https://%s:4567/rest".formatted(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            return "https://localhost:4567/rest";
        }
    }

    private final KafkaPublisher publisher;
    private final SyncPoint syncPoint = SyncPoint.getSyncPoint();
    private final String topic;
    private final String myUri;

    private final Cache<String, Message> msgCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMillis(MSG_CACHE_TTL))
            .build();

    private final Cache<String, String> gcCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMillis(GC_CACHE_TTL))
            .removalListener(e -> {
                var sql = "SELECT * FROM Message m WHERE NOT EXISTS (SELECT 1 FROM InboxEntry e WHERE e.mid = m.id)" +
                        " AND m.id NOT LIKE '" + THIS_DOMAIN + "+%'";
                DB.transaction(h -> h.select(sql, Message.class).thenWith(h::deleteMany));
            })
            .build();

    private final ConcurrentHashMap<String, ExecutorService> jobExecutors = new ConcurrentHashMap<>();

    private KafkaMessages(String myUri) {
        this.myUri = myUri;
        this.topic = "messages_" + THIS_DOMAIN.replace('.', '_').replace('-', '_');

        createTopic(KAFKA_ADDR, topic);
        this.publisher = KafkaPublisher.createPublisher(KAFKA_ADDR);

        KafkaSubscriber.createSubscriber(KAFKA_ADDR, List.of(topic)).start(this::applyOperation);

        waitForCatchup();
    }

    private static void createTopic(String addr, String topic) {
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, addr,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000"))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
            Log.info("Kafka topic created: " + topic);
        } catch (ExecutionException e) {
            Log.info("Kafka topic already exists: " + topic);
        } catch (Exception e) {
            Log.warning("createTopic error: " + e.getMessage());
        }
    }

    private void waitForCatchup() {
        long endOffset = getEndOffset(KAFKA_ADDR, topic);
        if (endOffset > 0) {
            Log.info("Waiting for Kafka catchup to offset " + (endOffset - 1));
            syncPoint.waitForVersion(endOffset - 1);
            Log.info("Kafka catchup complete at version " + syncPoint.getVersion());
        }
    }

    private static long getEndOffset(String addr, String topic) {
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, addr,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000"))) {
            var tp = new TopicPartition(topic, 0);
            return admin.listOffsets(Map.of(tp, OffsetSpec.latest()))
                    .partitionResult(tp).get().offset();
        } catch (Exception e) {
            return 0;
        }
    }

    private void applyOperation(ConsumerRecord<String, String> record) {
        long offset = record.offset();
        try {
            var event = GSON.fromJson(record.value(), KafkaEvent.class);
            String result = switch (event.type) {
                case OP_DELIVER -> applyDeliver(event, offset);
                case OP_REMOTE_DELIVER -> { applyRemoteDeliver(event); yield null; }
                case OP_DELETE_INBOX -> { applyDeleteInbox(event.mid); yield null; }
                case OP_REMOVE_ENTRY -> { applyRemoveEntry(event.mid, event.name); yield null; }
                case OP_DELETE_USER -> { applyDeleteUser(event.name); yield null; }
                default -> null;
            };
            syncPoint.setResult(offset, result);
        } catch (Exception e) {
            Log.warning("applyOperation error at offset " + offset + ": " + e.getMessage());
            syncPoint.setResult(offset, null);
        }
    }

    private String applyDeliver(KafkaEvent event, long offset) {
        if (event.msg == null || event.recipients == null)
            return null;
        if (event.pwd != null) {
            var userResult = Clients.UsersClient.get().getUser(event.msg.senderName(), event.pwd);
            if (userResult.error() == Result.ErrorCode.FORBIDDEN
                    || userResult.error() == Result.ErrorCode.NOT_FOUND)
                return null;
        }
        if (event.msg.getDestination() == null || event.msg.getDestination().isEmpty())
            return null;

        String msgId = THIS_DOMAIN + "+" + offset;
        Message msg = event.msg;
        msg.setId(msgId);
        msgCache.put(cacheKey(event.msg), msg);
        msgCache.put(msgId, msg);
        persistDelivery(msg, event.recipients, event.localUnknown);
        return msgId;
    }

    private void applyRemoteDeliver(KafkaEvent event) {
        if (event.msg == null || event.recipients == null) return;

        if (event.msg.getDestination() == null) return;

        persistDelivery(event.msg, event.recipients, event.localUnknown);
        if (myUri.equals(event.originatorUri))
            notifyUnknownRecipients(event.msg, event.remoteUnknown);
    }

    private void persistDelivery(Message msg, List<String> recipients, List<String> localUnknown) {
        DB.transaction(h -> {
            h.updateOne(msg);
            for (String name : recipients)
                h.updateOne(new InboxEntry(msg.getId(), name));
            return ok();
        });
        for (String addr : localUnknown) {
            Message errMsg = msg.cloneWithUserNotFound(addr);
            DB.transaction(h -> {
                h.updateOne(errMsg);
                h.updateOne(new InboxEntry(errMsg.getId(), msg.senderName()));
                return ok();
            });
        }
    }

    private void applyDeleteInbox(String mid) {
        var sql = "SELECT * FROM InboxEntry e WHERE e.mid = '%s'".formatted(mid);
        DB.transaction(h -> {
            h.getOne(mid, Message.class).thenWith(h::deleteOne);
            return h.select(sql, InboxEntry.class).thenWith(h::deleteMany);
        });
    }

    private void applyRemoveEntry(String mid, String name) {
        DB.deleteOne(new InboxEntry(mid, name));
        gcCache.put(mid, mid);
    }

    private void applyDeleteUser(String name) {
        var sql = "SELECT * FROM InboxEntry e WHERE e.recipient = '%s'".formatted(name);
        DB.transaction(h -> h.select(sql, InboxEntry.class).thenWith(entries -> {
            h.deleteMany(entries);
            entries.forEach(e -> gcCache.put(e.getMid(), e.getMid()));
            return ok();
        }));
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        if (badParams(pwd, msg))
            return error(BAD_REQUEST);

        var cached = msgCache.getIfPresent(cacheKey(msg));
        if (cached != null)
            return ok(cached.getId());

        var userResult = Clients.UsersClient.get().getUser(msg.senderName(), pwd);
        if (!userResult.isOK())
            return error(userResult.error());
        var user = userResult.value();
        msg.setSender("%s <%s@%s>".formatted(user.getDisplayName(), user.getName(), user.getDomain()));

        var localAddrs = localAddrs(msg);
        var remoteAddrs = remoteAddrs(msg);

        var checkResult = Clients.AdminUsersClient.get().checkUsers(localAddrs);
        if (!checkResult.isOK())
            return error(checkResult.error());
        var unknownLocal = checkResult.value();
        var knownLocal = new HashSet<>(localAddrs);
        knownLocal.removeAll(unknownLocal);

        var event = new KafkaEvent();
        event.type = OP_DELIVER;
        event.msg = msg;
        event.pwd = pwd;
        event.recipients = knownLocal.stream().map(this::getName).toList();
        boolean senderIsLocal = isLocalAddress(msg.senderAddress());
        if (senderIsLocal)
            event.localUnknown = new ArrayList<>(unknownLocal);

        long offset = publisher.publish(topic, GSON.toJson(event));
        if (offset < 0)
            return error(INTERNAL_ERROR);
        String msgId = syncPoint.waitForResult(offset);
        if (msgId == null)
            return error(INTERNAL_ERROR);
        msg.setId(msgId);

        if (!senderIsLocal)
            notifyUnknownRecipients(msg, new ArrayList<>(unknownLocal));

        if (!remoteAddrs.isEmpty()) {
            remoteAddrs.stream()
                    .collect(Collectors.groupingBy(this::getDomain, Collectors.toSet()))
                    .forEach((domain, addrs) -> submitJob(domain, () -> {
                        var res = reTry(() -> Clients.AdminMessagesClient.get(domain).remotePostMessage(msg),
                                REMOTE_DEADLINE);
                        if (res.error() == Result.ErrorCode.TIMEOUT) {
                            for (String addr : addrs)
                                publishDeliver(
                                        new Message(null, msg.getSender(), msg.senderAddress(),
                                                "FAILED TO SEND " + msgId + " TO " + addr + ": TIMEOUT",
                                                msg.getContents()),
                                        msg.senderName());
                        }
                    }));
        }

        return ok(msgId);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        if (badParams(name, mid, pwd))
            return error(BAD_REQUEST);
        waitForClientVersion();
        return Clients.UsersClient.get().getUser(name, pwd)
                .then(() -> DB.getOne(new InboxEntry(mid, name), InboxEntry.class))
                .then(() -> DB.getOne(mid, Message.class));
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        if (badParams(name, pwd))
            return error(BAD_REQUEST);
        waitForClientVersion();
        var sql = "SELECT m.mid FROM InboxEntry m WHERE m.recipient = '%s'".formatted(name);
        return Clients.UsersClient.get().getUser(name, pwd)
                .then(() -> DB.select(sql, String.class));
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        if (badParams(name, pwd, query))
            return error(BAD_REQUEST);
        waitForClientVersion();
        String escapedQuery = query.toUpperCase().replace("'", "''");
        var sql = """
				SELECT m.id FROM Message m
				INNER JOIN InboxEntry e ON e.mid = m.id AND e.recipient = '%s'
				WHERE (upper(m.subject) LIKE '%%%s%%' OR upper(m.contents) LIKE '%%%s%%')
				""".formatted(name, escapedQuery, escapedQuery);
        return Clients.UsersClient.get().getUser(name, pwd)
                .then(() -> DB.select(sql, String.class));
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        if (badParams(name, mid, pwd))
            return error(BAD_REQUEST);

        var userResult = Clients.UsersClient.get().getUser(name, pwd);
        if (!userResult.isOK())
            return error(userResult.error());

        var event = new KafkaEvent();
        event.type = OP_REMOVE_ENTRY;
        event.mid = mid;
        event.name = name;
        return publishVoid(event);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        if (badParams(name, mid, pwd))
            return error(BAD_REQUEST);

        var userResult = Clients.UsersClient.get().getUser(name, pwd);
        if (!userResult.isOK())
            return error(userResult.error());

        if (mid.startsWith(THIS_DOMAIN + "+")) {
            try {
                long offset = Long.parseLong(mid.substring(THIS_DOMAIN.length() + 1));
                syncPoint.waitForVersion(offset);
            } catch (NumberFormatException ignored) {}
        }

        var cached = msgCache.getIfPresent(mid);
        var msgResult = cached != null ? ok(cached) : DB.getOne(mid, Message.class);
        if (!msgResult.isOK())
            return ok();
        var msg = msgResult.value();
        if (!name.equals(getName(msg.senderAddress())))
            return error(FORBIDDEN);

        var event = new KafkaEvent();
        event.type = OP_DELETE_INBOX;
        event.mid = mid;
        var res = publishVoid(event);
        if (!res.isOK())
            return res;

        msg.getDestination().stream()
                .map(this::getDomain)
                .filter(d -> !d.equals(THIS_DOMAIN))
                .collect(Collectors.toSet())
                .forEach(domain -> submitJob(domain, () ->
                        reTry(() -> Clients.AdminMessagesClient.get(domain).remoteDeleteMessage(mid),
                                REMOTE_DEADLINE)));
        return ok();
    }

    @Override
    public Result<Void> remotePostMessage(Message msg) {
        if (badParams(msg))
            return error(BAD_REQUEST);

        var localAddrs = localAddrs(msg);
        var checkResult = Clients.AdminUsersClient.get().checkUsers(localAddrs);
        if (!checkResult.isOK())
            return error(checkResult.error());
        var unknownLocal = checkResult.value();
        var knownLocal = new HashSet<>(localAddrs);
        knownLocal.removeAll(unknownLocal);

        var event = new KafkaEvent();
        event.type = OP_REMOTE_DELIVER;
        event.msg = msg;
        event.recipients = knownLocal.stream().map(this::getName).toList();
        event.originatorUri = myUri;
        boolean senderIsLocal = isLocalAddress(msg.senderAddress());
        if (senderIsLocal)
            event.localUnknown = new ArrayList<>(unknownLocal);
        else
            event.remoteUnknown = new ArrayList<>(unknownLocal);
        return publishVoid(event);
    }

    @Override
    public Result<Void> remoteDeleteMessage(String mid) {
        if (badParams(mid))
            return error(BAD_REQUEST);
        var event = new KafkaEvent();
        event.type = OP_DELETE_INBOX;
        event.mid = mid;
        return publishVoid(event);
    }

    @Override
    public Result<Void> remoteDeleteUserInbox(String name) {
        if (badParams(name))
            return error(BAD_REQUEST);
        var event = new KafkaEvent();
        event.type = OP_DELETE_USER;
        event.name = name;
        return publishVoid(event);
    }


    private Result<Void> publishVoid(KafkaEvent event) {
        long offset = publisher.publish(topic, GSON.toJson(event));
        if (offset < 0)
            return error(INTERNAL_ERROR);
        syncPoint.waitForVersion(offset);
        return ok();
    }


    private void waitForClientVersion() {
        Long v = VersionContext.get();
        if (v != null && v >= 0)
            syncPoint.waitForVersion(v);
    }

    private List<String> localAddrs(Message msg) {
        return msg.getDestination().stream().filter(this::isLocalAddress).toList();
    }

    private Set<String> remoteAddrs(Message msg) {
        return msg.getDestination().stream()
                .filter(Predicate.not(this::isLocalAddress))
                .collect(Collectors.toSet());
    }


    private void publishDeliver(Message msg, String recipientName) {
        var event = new KafkaEvent();
        event.type = OP_DELIVER;
        event.msg = msg;
        event.recipients = List.of(recipientName);
        publisher.publish(topic, GSON.toJson(event));
    }

    private void notifyUnknownRecipients(Message msg, Collection<String> unknownAddrs) {
        if (unknownAddrs.isEmpty())
            return;
        String senderDomain = getDomain(msg.senderAddress());
        for (String addr : unknownAddrs) {
            Message errMsg = msg.cloneWithUserNotFound(addr);
            submitJob(senderDomain, () ->
                    reTry(() -> Clients.AdminMessagesClient.get(senderDomain).remotePostMessage(errMsg),
                            REMOTE_DEADLINE));
        }
    }

    private void submitJob(String domain, Runnable job) {
        jobExecutors.computeIfAbsent(domain, d -> Executors.newCachedThreadPool()).submit(job);
    }

    private String cacheKey(Message msg) {
        return " " + msg.senderName() + "_" + msg.getCreationTime()
                + "_" + (msg.getSubject() != null ? msg.getSubject().hashCode() : 0);
    }
}
