package sd2526.trab.impl.kafka;

public class VersionContext {

    private static final ThreadLocal<Long> version = new ThreadLocal<>();

    public static void set(long v) {
        version.set(v);
    }

    public static Long get() {
        return version.get();
    }

    public static void clear() {
        version.remove();
    }
}