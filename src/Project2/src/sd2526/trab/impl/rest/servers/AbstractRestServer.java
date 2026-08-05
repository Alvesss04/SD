package sd2526.trab.impl.rest.servers;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;
import javax.net.ssl.SSLContext;



public abstract class AbstractRestServer extends AbstractServer {
	private static final String SERVER_BASE_URI = "https://%s:%s%s";
	private static final String REST_CTX = "/rest";

	protected AbstractRestServer(Logger log, String service, int port) {
		super(log, service, String.format(SERVER_BASE_URI, getHostname(), port, REST_CTX));
	}

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

	protected void start() {
		sd2526.trab.impl.db.Hibernate.getInstance();
		
		ResourceConfig config = new ResourceConfig();
		registerResources( config );
        config.register(ServerSecretFilter.class);
		try {
            String bindURI = String.format("https://0.0.0.0:%s%s", URI.create(serverURI).getPort(), REST_CTX);
            JdkHttpServerFactory.createHttpServer(URI.create(bindURI), config,SSLContext.getDefault());

            if (service != null)
                Discovery.getInstance().announce(serviceName(), super.serverURI);

            Log.info(String.format("%s Server ready @ %s\n", service, serverURI));
        }catch (Exception e){
            Log.severe("Failed to start REST server: " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	abstract void registerResources( ResourceConfig config );
}
