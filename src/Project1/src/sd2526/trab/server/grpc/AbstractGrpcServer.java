package sd2526.trab.server.grpc;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import sd2526.trab.Discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public abstract class AbstractGrpcServer {
    private static final String SERVER_BASE_URI = "grpc://%s:%s/grpc";

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    final protected int port;
    final protected Logger Log;
    final protected String service;
    final protected String domain;
    final protected String serverURI;

    protected AbstractGrpcServer(Logger log, String service, String domain, int port) throws UnknownHostException {
        this.Log = log;
        this.port = port;
        this.service = service;
        this.domain = domain;
        this.serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), port);
    }

    protected void start() {
        try {
            BindableService bindableService = createController();
            ServerCredentials cred = InsecureServerCredentials.create();
            Server server = Grpc.newServerBuilderForPort(port, cred).addService(bindableService).build();

            server.start();
            Discovery.announce(String.format("%s@%s", service, domain), serverURI);
            Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));
            server.awaitTermination();

        } catch (Exception e) {
            Log.severe("Failed to start gRPC server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    abstract protected BindableService createController();
}