package sd2526.trab;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Discovery {
    private static Logger Log = Logger.getLogger(Discovery.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    static final public InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
    static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;
    static final int DISCOVERY_RETRY_TIMEOUT = 5000;
    static final int MAX_DATAGRAM_SIZE = 65536;
    private static final String DELIMITER = "\t";

    private static final Map<String, Set<URI>> knownServices = new HashMap<>();
    private static final Map<URI, Long> lastSeen = new HashMap<>();

    private static MulticastSocket ms;
    private static final ScheduledExecutorService backgroundTasks = Executors.newScheduledThreadPool(2);

    static {
        try {
            ms = new MulticastSocket(DISCOVERY_ADDR.getPort());
            ms.joinGroup(DISCOVERY_ADDR, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
            startListener();
        } catch (Exception e) {
            Log.severe("Erro ao iniciar Discovery: " + e.getMessage());
        }
    }

    private static void startListener() {
        backgroundTasks.execute(() -> {
            DatagramPacket pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
            for (;;) {
                try {
                    pkt.setLength(MAX_DATAGRAM_SIZE);
                    ms.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength());
                    String[] msgElems = msg.split(DELIMITER);

                    if (msgElems.length == 2) {
                        String receivedServiceName = msgElems[0];
                        URI receivedURI = URI.create(msgElems[1]);

                        synchronized (knownServices) {
                            knownServices.computeIfAbsent(receivedServiceName, k -> new HashSet<>()).add(receivedURI);
                            lastSeen.put(receivedURI, System.currentTimeMillis());
                            knownServices.notifyAll();
                        }
                    }
                } catch (IOException e) {
                }
            }
        });
    }

    public static void announce(String serviceName, String serviceURI) {
        Log.info(String.format("Starting Discovery announcements para: %s -> %s", serviceName, serviceURI));
        byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
        DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, DISCOVERY_ADDR);
        backgroundTasks.scheduleAtFixedRate(() -> {
            try {
                ms.send(announcePkt);
            } catch (Exception e) {
            }
        }, 0, DISCOVERY_ANNOUNCE_PERIOD, TimeUnit.MILLISECONDS);
    }

    public static URI[] knownUrisOf(String serviceName, int minReplies) {
        synchronized (knownServices) {
            while (true) {
                Set<URI> uris = knownServices.getOrDefault(serviceName, new HashSet<>());
                if (uris.size() >= minReplies) {
                    return uris.toArray(new URI[0]);
                }
                try {
                    knownServices.wait(DISCOVERY_RETRY_TIMEOUT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}