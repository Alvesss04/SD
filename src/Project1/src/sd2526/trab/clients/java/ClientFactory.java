package sd2526.trab.clients.java;

public interface ClientFactory<T> {
    public T get(String domain);
}