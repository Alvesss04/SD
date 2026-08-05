package sd2526.trab.impl.kafka;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {

    private final ConcurrentHashMap<Long, String> result = new ConcurrentHashMap<>();
    private long version = -1;

    private SyncPoint() {}

    private static SyncPoint instance;

    public static synchronized SyncPoint getSyncPoint() {
        if (instance == null)
            instance = new SyncPoint();
        return instance;
    }

    public synchronized String waitForResult(long n) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (version < n && System.currentTimeMillis() < deadline) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return result.remove(n);
    }

    public synchronized void waitForVersion(long n) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (version < n && System.currentTimeMillis() < deadline) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void setResult(long n, String res) {
        if (n <= version)
            return;
        if (res != null)
            result.put(n, res);
        version = n;
        notifyAll();
    }

    public synchronized long getVersion() {
        return version;
    }
}