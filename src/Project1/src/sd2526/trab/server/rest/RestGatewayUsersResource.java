package sd2526.trab.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import sd2526.trab.api.User;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.clients.java.Clients;
import java.util.List;

//@Singleton
@Path(RestUsers.PATH)
public class RestGatewayUsersResource extends RestResource implements RestUsers {

    private final String domain;

    public RestGatewayUsersResource(String domain) {
        this.domain = domain;
    }

    @Override
    public String postUser(User user) {
        return unwrapResultOrThrow(Clients.UsersClient.get(domain).postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return unwrapResultOrThrow(Clients.UsersClient.get(domain).getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        return unwrapResultOrThrow(Clients.UsersClient.get(domain).updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return unwrapResultOrThrow(Clients.UsersClient.get(domain).deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return unwrapResultOrThrow(Clients.UsersClient.get(domain).searchUsers(name, pwd, pattern));
    }
}