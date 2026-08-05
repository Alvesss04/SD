package sd2526.trab.server.java;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.persistence.Hibernate;

import java.util.List;
import java.util.logging.Logger;

public class JavaUsers implements Users {

    private static final Logger Log = Logger.getLogger(JavaUsers.class.getName());

    private String domain;
    private Hibernate hibernate;

    public JavaUsers(String domain) {
        hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    public Result<String> postUser(User user) {
        Log.info("postUser : " + user);

        if(user.getName() == null || user.getPwd() == null ||
                user.getDisplayName() == null || user.getDomain() == null) {

            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        if(!user.getDomain().equals(domain)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        try {
            User existing = hibernate.get(User.class, user.getName());
            if (existing != null) {
                if (existing.getPwd().equals(user.getPwd()) &&
                        existing.getDisplayName().equals(user.getDisplayName())) {
                    return Result.ok(user.getName() + "@" + domain);
                } else {
                    return Result.error(Result.ErrorCode.CONFLICT);
                }
            }
            hibernate.persist(user);
        }
        catch (Exception e) {
            e.printStackTrace();

            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok(user.getName() + "@" + domain);
    }

    public Result<User> getUser(String name, String pwd) {
        if(name == null || pwd == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user = null;
        try {
            user = hibernate.get(User.class, name);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        if (user == null || !user.getPwd().equals(pwd)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    public Result<User> updateUser(String name, String pwd, User info) {
        if(name == null || pwd == null || (info.getName() != null && !name.equals(info.getName()))) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user = null;
        try {
            user = hibernate.get(User.class, name);

            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }

            if(info.getPwd() != null) {
                user.setPwd(info.getPwd());
            }

            if(info.getDomain() != null) {
                user.setDomain(info.getDomain());
            }

            if(info.getDisplayName() != null) {
                user.setDisplayName(info.getDisplayName());
            }

            hibernate.update(user);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    public Result<User> deleteUser(String name, String pwd) {
        if(name == null || pwd == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user = null;
        try {
            user = hibernate.get(User.class, name);

            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            hibernate.delete(User.class, name);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        if(name == null || pwd == null || query == null)  {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user = null;
        List<User> users = null;
        try {
            user = hibernate.get(User.class, name);

            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }

            String queryString = "SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER('%" + query + "%')";
            users = hibernate.jpql(queryString, User.class);

            for(User userListed : users) {
                userListed.setPwd("");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(users);
    }
}

