package sd2526.trab.server.java;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.clients.java.Clients;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class JavaMessages implements Messages {

    private static final long MAX_RETRY_TIME = 60_000L;
    private static final long THREAD_PAUSE = 5000;

    private static final Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final Hibernate hibernate;
    private final String domain;
    private final Users usersService;
    private final Map<String, ExecutorService> domainExecutors = new ConcurrentHashMap<>();

    public JavaMessages(String domain, Users usersService) {
        this.domain = domain;
        this.hibernate = Hibernate.getInstance();
        this.usersService = usersService;
    }
    private ExecutorService getExecutorForDomain(String targetDomain) {
        return domainExecutors.computeIfAbsent(targetDomain, k -> Executors.newSingleThreadExecutor());
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("postMessage : " + msg);

        if (msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        String senderDomain = parseDomain(msg.getSender());
        String userId = parseName(msg.getSender());
        boolean isBounceBack = msg.getId() != null && msg.getSubject() != null && msg.getSubject().startsWith("FAILED TO SEND");

        if (senderDomain.equals(this.domain) && !isBounceBack) {
            if (pwd == null) return Result.error(Result.ErrorCode.BAD_REQUEST);

            Result<User> userRes = usersService.getUser(userId, pwd);
            if (!userRes.isOK()) {
                return Result.error(userRes.error());
            }

            if (msg.getId() == null) {
                msg.setId(UUID.randomUUID().toString());
            }
            msg.setCreationTime(System.currentTimeMillis());
            User sender = userRes.value();
            msg.setSender(sender.getDisplayName() + " <" + userId + "@" + domain + ">");
        }

        if (msg.getId() != null) {
            Message existing = hibernate.get(Message.class, msg.getId());
            if (existing != null) {
                Log.info("Message already exists, idempotence returning OK");
                return Result.ok(existing.getId());
            }
        }

        try {
            hibernate.persist(msg);
        } catch (Exception e) {
            Log.severe("Failed to persist message: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        if (!isBounceBack) {
            getExecutorForDomain(this.domain).submit(() -> checkAndNotifyUnknownLocalUsers(pwd, msg));
        }
        if (senderDomain.equals(this.domain) && !isBounceBack) {
            propagateMessage(pwd, msg);
        }

        return Result.ok(msg.getId());
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        if (!usersService.getUser(name, pwd).isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Message msg = hibernate.get(Message.class, mid);
        if (msg == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        String userEmail = name + "@" + domain;
        if (!msg.getDestination().contains(userEmail))
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        return Result.ok(msg);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        if (!usersService.getUser(name, pwd).isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        String userEmail = name + "@" + domain;
        List<String> messageIds = hibernate.jpql(
                "SELECT m.id FROM Message m JOIN m.destination d WHERE d = :email",
                String.class,
                Map.of("email", userEmail));

        return Result.ok(messageIds);
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        if (!usersService.getUser(name, pwd).isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Message msg = hibernate.get(Message.class, mid);
        if (msg == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        String userEmail = name + "@" + domain;
        if (!msg.getDestination().contains(userEmail))
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        msg.getDestination().remove(userEmail);

        hibernate.update(msg);

        return Result.ok();
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        Message msg = hibernate.get(Message.class, mid);

        if (msg == null) {
            Message tombstone = new Message();
            tombstone.setId(mid);
            tombstone.setSender(name + "@" + this.domain);
            tombstone.setSubject("");
            tombstone.setContents("");
            tombstone.setDestination(new HashSet<>());
            tombstone.setCreationTime(0);
            try {
                hibernate.persist(tombstone);
            } catch (Exception e) {
                Log.warning("Tombstone failed for " + mid + ". Forcing a retry!");
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return Result.ok();
        }

        String senderName = parseName(msg.getSender());
        String senderDomain = parseDomain(msg.getSender());

        if (!name.equals(senderName)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        if (senderDomain.equals(this.domain)) {
            if (!usersService.getUser(name, pwd).isOK()) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            long elapsed = System.currentTimeMillis() - msg.getCreationTime();
            if (elapsed <= MAX_RETRY_TIME || msg.getCreationTime() == 0) {
                Set<String> remoteDomains = new HashSet<>();
                for (String dest : msg.getDestination()) {
                    String destDomain = parseDomain(dest);
                    if (!destDomain.equals(this.domain)) {
                        remoteDomains.add(destDomain);
                    }
                }
                String msgId = msg.getId();

                msg.getDestination().clear();
                hibernate.update(msg);

                propagateDelete(remoteDomains, senderName, msgId, pwd);
            }
        } else {
            msg.getDestination().clear();
            hibernate.update(msg);
        }

        return Result.ok();
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        if (!usersService.getUser(name, pwd).isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        String userEmail = name + "@" + domain;
        String likePattern = "%" + query.toLowerCase() + "%";

        List<String> messageIds = hibernate.jpql(
                "SELECT m.id FROM Message m JOIN m.destination d " +
                        "WHERE d = :email " +
                        "AND (LOWER(m.subject) LIKE :pattern OR LOWER(m.contents) LIKE :pattern)",
                String.class,
                Map.of("email", userEmail, "pattern", likePattern));

        return Result.ok(messageIds);
    }

    private void propagateMessage(String pwd, Message msg) {
        Set<String> remoteDomains = new HashSet<>();
        for (String dest : msg.getDestination()) {
            String destDomain = parseDomain(dest);
            if (!destDomain.equals(this.domain)) {
                remoteDomains.add(destDomain);
            }
        }

        long startTime = System.currentTimeMillis();

        for (String remoteDomain : remoteDomains) {
            getExecutorForDomain(remoteDomain).submit(() -> {
                boolean success = false;
                while (!success) {
                    if (System.currentTimeMillis() - startTime > MAX_RETRY_TIME) {
                        Log.warning("Timeout propagating to " + remoteDomain + ". Giving up and sending bounce-backs.");

                        String senderEmail = parseName(msg.getSender()) + "@" + parseDomain(msg.getSender());

                        for (String dest : msg.getDestination()) {
                            if (parseDomain(dest).equals(remoteDomain)) {
                                Message errorMsg = new Message();
                                errorMsg.setId(msg.getId() + "." + dest);
                                errorMsg.setSender(msg.getSender());
                                errorMsg.setDestination(new HashSet<>(Set.of(senderEmail)));
                                errorMsg.setSubject("FAILED TO SEND " + msg.getId() + " TO " + dest + ": TIMEOUT");
                                errorMsg.setContents(msg.getContents());
                                errorMsg.setCreationTime(System.currentTimeMillis());

                                try {
                                    hibernate.persist(errorMsg);
                                } catch (Exception e) {
                                    Log.warning("Failed to persist timeout error message for " + dest);
                                }
                            }
                        }
                        break;
                    }

                    try {
                        Log.info("Propagating message to remote domain: " + remoteDomain);
                        Result<String> res = Clients.MessagesClient.get(remoteDomain).postMessage(pwd, msg);

                        if (res.isOK()) {
                            success = true;
                        } else {
                            Log.warning("Failed to propagate message to " + remoteDomain + ". Error: " + res.error() + ". Retrying in 5 seconds...");
                            try { Thread.sleep(THREAD_PAUSE); } catch (InterruptedException ignored) {}
                        }
                    } catch (Exception e) {
                        Log.warning("Exception propagating message to " + remoteDomain + ". Retrying in 5 seconds...");
                        try { Thread.sleep(THREAD_PAUSE); } catch (InterruptedException ignored) {}
                    }
                }
            });
        }
    }

    private void propagateDelete(Set<String> remoteDomains, String senderName, String msgId, String pwd) {
        long startTime = System.currentTimeMillis();

        for (String remoteDomain : remoteDomains) {
            getExecutorForDomain(remoteDomain).submit(() -> {
                boolean success = false;
                while (!success) {
                    if (System.currentTimeMillis() - startTime > MAX_RETRY_TIME) {
                        Log.warning("Timeout propagating delete to " + remoteDomain + ". Giving up.");
                        break;
                    }

                    try {
                        Log.info("Propagating delete to remote domain: " + remoteDomain);
                        Result<Void> res = Clients.MessagesClient.get(remoteDomain).deleteMessage(senderName, msgId, pwd);

                        if (res.isOK()) {
                            success = true;
                        } else {
                            Log.warning("Failed to propagate delete to " + remoteDomain + ". Error: " + res.error() + ". Retrying in 5 seconds...");
                            try { Thread.sleep(THREAD_PAUSE); } catch (InterruptedException ignored) {}
                        }
                    } catch (Exception e) {
                        Log.warning("Exception propagating delete to " + remoteDomain + ". Retrying in 5 seconds...");
                        try { Thread.sleep(THREAD_PAUSE); } catch (InterruptedException ignored) {}
                    }
                }
            });
        }
    }

    private String parseName(String email) {
        if (email == null) return null;

        if (email.contains("<") && email.contains(">")) {
            String cleanEmail = email.substring(email.indexOf('<') + 1, email.indexOf('>'));
            return cleanEmail.substring(0, cleanEmail.indexOf('@'));
        }
        if (email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return email;
    }

    private String parseDomain(String email) {
        if (email == null) return domain;

        if (email.contains("<") && email.contains(">")) {
            String cleanEmail = email.substring(email.indexOf('<') + 1, email.indexOf('>'));
            return cleanEmail.substring(cleanEmail.indexOf('@') + 1);
        }
        if (email.contains("@")) {
            return email.substring(email.indexOf('@') + 1);
        }
        return domain;
    }

    private void checkAndNotifyUnknownLocalUsers(String pwd, Message msg) {
        String senderEmail = parseName(msg.getSender()) + "@" + parseDomain(msg.getSender());
        String senderName = parseName(msg.getSender());
        String senderDomain = parseDomain(msg.getSender());

        String searchUser = senderName;
        String searchPwd = pwd;

        if (!senderDomain.equals(this.domain) || pwd == null) {
            searchUser = "sys_msg_checker";
            searchPwd = "sys_pwd";
            User sysUser = new User();
            sysUser.setName(searchUser);
            sysUser.setPwd(searchPwd);
            sysUser.setDomain(this.domain);
            sysUser.setDisplayName("System");

            try {
                usersService.postUser(sysUser);
            } catch (Exception e) {
                Log.info("System user already exists, proceeding...");
            }
        }

        for (String dest : msg.getDestination()) {
            if (parseDomain(dest).equals(this.domain)) {
                String destName = parseName(dest);
                if (destName.equals(senderName) && senderDomain.equals(this.domain)) {
                    continue;
                }
                Result<List<User>> searchRes = usersService.searchUsers(searchUser, searchPwd, destName);

                boolean userExists = false;
                if (searchRes.isOK() && searchRes.value() != null) {
                    for (User u : searchRes.value()) {
                        if (u.getName().equalsIgnoreCase(destName)) {
                            userExists = true;
                            break;
                        }
                    }
                }

                if (!userExists) {
                    Message errorMsg = new Message();
                    errorMsg.setId(msg.getId() + "." + dest);
                    errorMsg.setSender(msg.getSender());
                    errorMsg.setDestination(new HashSet<>(Set.of(senderEmail)));
                    errorMsg.setSubject("FAILED TO SEND " + msg.getId() + " TO " + dest + ": UNKNOWN USER");
                    errorMsg.setContents(msg.getContents());
                    errorMsg.setCreationTime(System.currentTimeMillis());

                    try {
                        hibernate.persist(errorMsg);
                    } catch (Exception e) {
                        Log.warning("Failed to persist error message for " + dest);
                    }

                    if (!senderDomain.equals(this.domain)) {
                        getExecutorForDomain(senderDomain).submit(() -> {
                            try {
                                Clients.MessagesClient.get(senderDomain).postMessage("", errorMsg);
                            } catch (Exception e) {
                                Log.warning("Failed to propagate error back to remote sender: " + e.getMessage());
                            }
                        });
                    }
                }
            }
        }
    }
}