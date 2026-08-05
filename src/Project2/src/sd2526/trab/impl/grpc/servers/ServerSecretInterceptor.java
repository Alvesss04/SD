package sd2526.trab.impl.grpc.servers;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.logging.Logger;

public class ServerSecretInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(ServerSecretInterceptor.class.getName());
    private static final Metadata.Key<String> SECRET_HEADER = Metadata.Key.of("x-server-secret", Metadata.ASCII_STRING_MARSHALLER);
    private static final String EXPECTED_SECRET = System.getProperty("secret", "");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        if (!EXPECTED_SECRET.isEmpty() && methodName.contains("Admin")) {
            String providedSecret = headers.get(SECRET_HEADER);
            if (providedSecret == null || !providedSecret.equals(EXPECTED_SECRET)) {
                logger.warning("Admin access denied, method: " + methodName);
                call.close(Status.PERMISSION_DENIED.withDescription("Invalid server secret"), new Metadata());
                return new ServerCall.Listener<ReqT>() {};
            }
        }
        return next.startCall(call, headers);
    }
}