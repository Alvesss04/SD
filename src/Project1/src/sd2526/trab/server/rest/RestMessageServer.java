package sd2526.trab.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.clients.java.Clients;
import sd2526.trab.server.java.JavaMessages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class RestMessageServer extends AbstractRestServer {

    private static final int PORT = 8080;

    private static final Logger log = Logger.getLogger(RestMessageServer.class.getName());

    private final String domain;

    RestMessageServer(String domain) throws UnknownHostException{
        super(log, JavaMessages.SERVICE_NAME,domain,PORT);
        this.domain = domain;
    }

    @Override
    void registerResources(ResourceConfig config) {
        var usersClient = Clients.UsersClient.get(domain);

        var messagesLogic = new JavaMessages(domain, usersClient);
        config.register(new RestMessageResource(messagesLogic));
    }

    public static void main(String[] args) throws Exception {
        String hostname = InetAddress.getLocalHost() .getHostName();
        String domain = hostname.substring(hostname.indexOf(".") + 1);
        new RestMessageServer(domain).start();

        Thread.currentThread().join();
    }
}
