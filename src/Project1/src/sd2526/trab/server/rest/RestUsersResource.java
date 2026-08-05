package sd2526.trab.server.rest;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.server.java.JavaUsers;

import java.util.List;

public class RestUsersResource extends RestResource implements RestUsers {

    private final Users impl;

    public RestUsersResource(String domain) {

        impl = new JavaUsers(domain);
    }

    @Override
    public String postUser(User user) {
        return super.unwrapResultOrThrow( impl.postUser( user ) );
    }

    @Override
    public User getUser(String name, String pwd) {
        return super.unwrapResultOrThrow( impl.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        return super.unwrapResultOrThrow( impl.updateUser(name, pwd, user));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return super.unwrapResultOrThrow( impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return super.unwrapResultOrThrow( impl.searchUsers(name, pwd, pattern ));
    }
}