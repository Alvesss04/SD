package sd2526.trab.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class RestUsersClient extends RestClient implements Users {

    private static Logger Log = Logger.getLogger(RestUsersClient.class.getName());

    public RestUsersClient( URI serverURI ) {
        super( serverURI, Log );

        target = super.target.path( RestUsers.PATH );
    }

    @Override
    public Result<String> postUser(User user) {
        return super.reTry( () -> doPostUser( user ) );
    }

    private Result<String> doPostUser(User user) {
        Response r = target.request()
                .accept( MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));

        return super.processResponse(r, String.class);
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        return super.reTry( () -> doGetUser( userId, pwd ));
    }

    private Result<User> doGetUser(String userId, String pwd) {
        Response r = target.path( userId )
                .queryParam(RestUsers.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse( r, User.class );
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        return super.reTry( () -> doUpdateUser( userId, password, user ));
    }

    private Result<User> doUpdateUser(String userId, String password, User user) {
        Response r = target.path(userId)
                .queryParam(RestUsers.PWD, password)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(user, MediaType.APPLICATION_JSON));

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return super.reTry( () -> doDeleteUser(userId, password));
    }

    private Result<User> doDeleteUser(String userId, String password) {
        Response r = target.path(userId)
                .queryParam(RestUsers.PWD, password)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String pattern) {
        return super.reTry( () -> doSearchUsers(name,pwd,pattern));
    }

    private Result<List<User>> doSearchUsers(String name, String pwd, String pattern) {
        Response r = target
                .queryParam(RestUsers.NAME,name)
                .queryParam(RestUsers.PWD,pwd)
                .queryParam(RestUsers.QUERY, pattern)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse(r,  new GenericType<List<User>>() {});
    }
}
