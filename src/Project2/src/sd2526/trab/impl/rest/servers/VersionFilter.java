package sd2526.trab.impl.rest.servers;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import sd2526.trab.impl.kafka.SyncPoint;
import sd2526.trab.impl.kafka.VersionContext;

@Provider
public class VersionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String VERSION_HEADER = "X-MESSAGES-VERSION";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String v = requestContext.getHeaderString(VERSION_HEADER);
        if (v != null) {
            try {
                VersionContext.set(Long.parseLong(v));
            } catch (NumberFormatException e) {
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        long version = SyncPoint.getSyncPoint().getVersion();
        if (version >= 0)
            responseContext.getHeaders().add(VERSION_HEADER, version);
        VersionContext.clear();
    }
}