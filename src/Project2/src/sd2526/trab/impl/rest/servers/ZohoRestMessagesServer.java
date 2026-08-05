package sd2526.trab.impl.rest.servers;

import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.external.ZohoRestMessagesResource;

import java.util.logging.Logger;

public class ZohoRestMessagesServer extends AbstractRestServer {
    public static final int PORT = 8901;

    private static Logger Log = Logger.getLogger(ZohoRestMessagesServer.class.getName());

    private static String arg;

    ZohoRestMessagesServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    void registerResources(ResourceConfig config) {
        config.register(ZohoRestMessagesResource.class);
    }

    public static String getArg() {
        return arg;
    }

    public static void main(String[] args) {
        if(args.length > 0){
            arg = args[0];
        }
        new ZohoRestMessagesServer().start();
    }
}
