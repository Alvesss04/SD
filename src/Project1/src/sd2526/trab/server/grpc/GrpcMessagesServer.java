package sd2526.trab.server.grpc;

import io.grpc.*;
import sd2526.trab.api.java.Messages;
import sd2526.trab.clients.java.Clients;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class GrpcMessagesServer extends AbstractGrpcServer {

    public static final int PORT = 8084;

    private static Logger Log = Logger.getLogger(GrpcMessagesServer.class.getName());

    public GrpcMessagesServer(String domain) throws UnknownHostException  {
        super( Log, Messages.SERVICE_NAME, domain, PORT);
    }

    public static void main(String[] args) throws Exception {

        try {
        String hostname = InetAddress.getLocalHost() .getHostName();
        String domain = hostname.substring(hostname.indexOf(".") + 1);

        new GrpcMessagesServer(domain).start();
        }
        catch (Exception e) {
        Log.severe("Failed to start server: " + e.getMessage());
        e.printStackTrace();
    }
    }

    @Override
    protected BindableService createController() {
        var usersClient = Clients.UsersClient.get(domain);
        return new GrpcMessagesController(domain, usersClient);
    }
}
