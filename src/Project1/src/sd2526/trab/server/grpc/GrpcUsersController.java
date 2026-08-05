package sd2526.trab.server.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users;
import sd2526.trab.server.java.JavaUsers;

import java.util.List;

import static sd2526.trab.server.grpc.DataModelAdaptor.GrpcUser_to_User;
import static sd2526.trab.server.grpc.DataModelAdaptor.User_to_GrpcUser;

public class GrpcUsersController extends GrpcController implements GrpcUsersGrpc.AsyncService, BindableService {

    private final JavaUsers impl;
    public GrpcUsersController(JavaUsers impl) {
        this.impl = impl;
    }
    @Override
    public ServerServiceDefinition bindService() {
        return GrpcUsersGrpc.bindService(this);
    }

    @Override
    public void postUser(Users.GrpcUser request, StreamObserver<Users.PostUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.postUser(GrpcUser_to_User(request)),
                (address) -> Users.PostUserResult.newBuilder().setUserAddress(address).build());
    }

    @Override
    public void getUser(Users.GetUserArgs request, StreamObserver<Users.GetUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getUser(request.getName(), request.getPwd()),
                (user) -> Users.GetUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void updateUser(Users.UpdateUserArgs request, StreamObserver<Users.UpdateUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.updateUser(request.getName(), request.getPwd(), GrpcUser_to_User(request.getInfo())),
                (user) -> Users.UpdateUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void deleteUser(Users.DeleteUserArgs request, StreamObserver<Users.DeleteUserResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.deleteUser(request.getName(), request.getPwd()),
                (user) -> Users.DeleteUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
    }

    @Override
    public void searchUsers(Users.SearchUsersArgs request, StreamObserver<Users.GrpcUser> responseObserver) {
        Result<List<User>> res = impl.searchUsers(request.getName(), request.getPwd(), request.getQuery());

        if (res.isOK()) {
            for (User u : res.value()) {
                responseObserver.onNext(User_to_GrpcUser(u));
            }
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(errorCodeToStatus(res.error()));
        }
    }
}