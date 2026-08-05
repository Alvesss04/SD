package sd2526.trab.impl.rest.servers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class ServerSecretFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(ServerSecretFilter.class.getName());
    private static final String SECRET_HEADER = "X-Server-Secret";
    private static final String EXPECTED_SECRET = System.getProperty("secret", "");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (EXPECTED_SECRET.isEmpty())
            return;
        String path = requestContext.getUriInfo().getPath();
        if (path.contains("/admin")) {
            String providedSecret = requestContext.getHeaderString("X-Server-Secret");
            if (providedSecret == null || !providedSecret.equals(System.getProperty("secret"))) {
                logger.warning("Admin access denied, path: " + path);
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("Access Denied")
                        .build());
            }
        }
    }
}
