package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;

public class RestMessagesRepServer extends AbstractRestServer {

    public static final int PORT = 4567;

    private static Logger Log = Logger.getLogger(RestMessagesRepServer.class.getName());

    RestMessagesRepServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.registerInstances(new RestMessagesRepResource());
        config.register(VersionFilter.class);
    }

    public static void main(String[] args) {
        new RestMessagesRepServer().start();
    }
}