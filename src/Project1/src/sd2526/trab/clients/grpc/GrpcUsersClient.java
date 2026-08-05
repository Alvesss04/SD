package sd2526.trab.clients.grpc;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.grpc.DataModelAdaptor;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.UpdateUserResult;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserResult;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class GrpcUsersClient extends GrpcClient implements Users {

    private static Logger Log = Logger.getLogger(GrpcUsersClient.class.getName());

    final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        super( serverURI, Log );
        stub = GrpcUsersGrpc.newBlockingStub( channel );
    }

    protected GrpcUsersClient(URI serverURI, Logger logger, GrpcUsersGrpc.GrpcUsersBlockingStub stub) {
        super(serverURI, logger);
        this.stub = stub;
    }

    @Override
    public Result<String> postUser(User user) {
        return super.processResponse( () -> stub.postUser(DataModelAdaptor.User_to_GrpcUser(user)).getUserAddress() );
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        GetUserResult res = stub.getUser(GetUserArgs.newBuilder()
                .setName(name).setPwd(pwd)
                .build());

        return super.processResponse( () -> DataModelAdaptor.GrpcUser_to_User(res.getUser()) );
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        UpdateUserResult res = stub.updateUser(UpdateUserArgs.newBuilder().setName(name).setPwd(pwd).setInfo(DataModelAdaptor.User_to_GrpcUser(info)).build());
        return super.processResponse(() -> DataModelAdaptor.GrpcUser_to_User(res.getUser()));
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        DeleteUserResult res = stub.deleteUser(DeleteUserArgs.newBuilder().setName(name).setPwd(pwd).build());
        return super.processResponse(() -> DataModelAdaptor.GrpcUser_to_User(res.getUser()));
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        SearchUsersArgs args = SearchUsersArgs.newBuilder()
                .setName(name)
                .setPwd(pwd)
                .setQuery(query)
                .build();
        return super.processResponse( () -> {
            Iterator<GrpcUser> res = stub.searchUsers(args);
            List<User> userList = new ArrayList<>();
            while (res.hasNext()) {
                GrpcUser grpcUser = res.next();
                userList.add(DataModelAdaptor.GrpcUser_to_User(grpcUser));
            }
            return userList;
        } );
    }
}
