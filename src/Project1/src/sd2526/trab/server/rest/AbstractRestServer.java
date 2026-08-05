package sd2526.trab.server.rest;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2526.trab.Discovery;


public abstract class AbstractRestServer {
    private static final String SERVER_BASE_URI = "http://%s:%s/rest";

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }


    final protected int port;
    final protected Logger Log;
    final protected String service;
    final protected String domain;
    final protected String serverURI;

    protected AbstractRestServer(Logger log, String service,String domain, int port) throws UnknownHostException {
        this.Log = log;
        this.port = port;
        this.service = service;
        this.domain = domain;
        this.serverURI = SERVER_BASE_URI.formatted(InetAddress.getLocalHost().getHostName(), port);
    }

    protected void start() {

        ResourceConfig config = new ResourceConfig();

        registerResources( config );

        var uri = URI.create("http://0.0.0.0:%s/rest".formatted(port));
        System.out.println( uri );
        JdkHttpServerFactory.createHttpServer( uri, config);
        Discovery.announce("%s@%s".formatted(service, domain), serverURI );

        Log.info(String.format("%s Server ready @ %s\n",  service, serverURI));
    }

    abstract void registerResources( ResourceConfig config );
}
