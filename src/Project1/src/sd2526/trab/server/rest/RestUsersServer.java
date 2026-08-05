package sd2526.trab.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.java.Users;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class RestUsersServer extends AbstractRestServer {

    public static final int PORT = 8081;

    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    private final String domain;

    RestUsersServer(String domain) throws UnknownHostException {
        super( Log, Users.SERVICE_NAME, domain, PORT);
        this.domain = domain;
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(new RestUsersResource(domain));
    }

    public static void main(String[] args) throws Exception {
        String hostname = InetAddress.getLocalHost() .getHostName();
        String domain = hostname.substring(hostname.indexOf(".") + 1);
        new RestUsersServer(domain).start();

        Thread.currentThread().join();
    }

}
