package sd2526.trab.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class RestGatewayServer extends AbstractRestServer {

    private static final int PORT = 8082;
    private static final Logger log = Logger.getLogger(RestGatewayServer.class.getName());
    private final String domain;

    RestGatewayServer(String domain) throws UnknownHostException{
        super(log, "Gateway", domain, PORT);
        this.domain = domain;
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(new RestGatewayUsersResource(domain));
        config.register(new RestGatewayMessagesResource(domain));
    }

    public static void main(String[] args) throws Exception {
        String hostname = InetAddress.getLocalHost().getHostName();
        String domain = hostname.substring(hostname.indexOf(".") + 1);

        new RestGatewayServer(domain).start();

        Thread.currentThread().join();
    }
}