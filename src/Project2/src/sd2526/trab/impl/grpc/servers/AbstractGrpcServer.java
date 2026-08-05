package sd2526.trab.impl.grpc.servers;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.List;
import java.util.logging.Logger;

import io.grpc.Server;
import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import javax.net.ssl.KeyManagerFactory;


public abstract class AbstractGrpcServer extends AbstractServer {
	private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

	private static final String GRPC_CTX = "/grpc";

	protected final Server server;

	protected AbstractGrpcServer(Logger log, String service, int port) {
		super(log, service, String.format(SERVER_BASE_URI, getHostname(), port, GRPC_CTX));
		Server sv = null;

        try {
            String keyStoreFilename = System.getProperty("javax.net.ssl.keyStore");
            String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            try(FileInputStream input = new FileInputStream(keyStoreFilename)) {
                keystore.load(input, keyStorePassword.toCharArray());
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keyStorePassword.toCharArray());

            SslContext context = GrpcSslContexts.configure(SslContextBuilder.forServer(keyManagerFactory)).build();

            var builder = NettyServerBuilder.forPort(port).sslContext(context).intercept(new ServerSecretInterceptor());
            for( var s : controllers( super.serverURI ) )
                builder.addService( s );

            sv = builder.build();

        } catch (Exception e) {
            Log.severe("Failed to initialize gRPC TLS Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

		this.server = sv;
		sd2526.trab.impl.db.Hibernate.getInstance();
	}

	protected abstract List<GrpcController> controllers( String uri );
	
	protected void start() throws IOException {
		
		Discovery.getInstance().announce(serviceName(), super.serverURI);
		
		Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));

		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread( () -> {
			System.err.println("*** shutting down gRPC server since JVM is shutting down");
			server.shutdownNow();
			System.err.println("*** server shut down");
		}));
	}
    private static String getHostname(){
        try{
            return InetAddress.getLocalHost().getHostName();
        }catch (Exception e){
            return "localhost";
        }
    }
	
}
