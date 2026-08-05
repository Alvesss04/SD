package sd2526.trab.server.grpc;

import io.grpc.BindableService;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.java.JavaUsers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class GrpcUsersServer extends AbstractGrpcServer {

    public static final int PORT = 8083;
    private static final Logger Log = Logger.getLogger(GrpcUsersServer.class.getName());
    private final String domain;

    public GrpcUsersServer(String domain) throws UnknownHostException {
        super(Log, Users.SERVICE_NAME, domain, PORT);
        this.domain = domain;
    }

    @Override
    protected BindableService createController() {
        return new GrpcUsersController(new JavaUsers(domain));
    }

    public static void main(String[] args) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String domain = hostname.substring(hostname.indexOf(".") + 1);
            new GrpcUsersServer(domain).start();

        } catch (Exception e) {
            Log.severe("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}