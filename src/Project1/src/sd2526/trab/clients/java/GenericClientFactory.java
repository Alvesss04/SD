package sd2526.trab.clients.java;

import java.net.URI;
import java.util.function.Function;
import sd2526.trab.Discovery;

public class GenericClientFactory<T> implements ClientFactory<T> {
    private static final String REST = "/rest";
    private static final String GRPC = "/grpc";
    private static final String DOMAIN_DELIMITER = "@";

    private final String serviceName;
    private final Function<URI, T> restClientFunc;
    private final Function<URI, T> grpcClientFunc;

    GenericClientFactory(String serviceName, Function<URI, T> restClientFunc, Function<URI, T> grpcClientFunc) {
        this.serviceName = serviceName;
        this.restClientFunc = restClientFunc;
        this.grpcClientFunc = grpcClientFunc;
    }

    @Override
    public T get(String domain) {
        var sn = "%s%s%s".formatted(serviceName, DOMAIN_DELIMITER, domain);
        var uris = Discovery.knownUrisOf(sn, 1);
        if (uris.length == 0) throw new RuntimeException("Service not found: " + sn);

        return newClient(uris[0]);
    }

    private T newClient(URI serverURI) {
        var path = serverURI.getPath();
        if (path.endsWith(REST))
            return restClientFunc.apply(serverURI);
        if (path.endsWith(GRPC))
            return grpcClientFunc.apply(serverURI);
        throw new RuntimeException("Unknown service type: " + serverURI);
    }
}