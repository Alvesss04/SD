package sd2526.trab.server.grpc;

import com.google.protobuf.Empty;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.java.JavaMessages;
import sd2526.trab.server.grpc.DataModelAdaptor;

import static sd2526.trab.server.grpc.DataModelAdaptor.GrpcMessage_to_Message;
import static sd2526.trab.server.grpc.DataModelAdaptor.Message_to_GrpcMessage;

public class GrpcMessagesController extends GrpcController implements GrpcMessagesGrpc.AsyncService, BindableService {

    private sd2526.trab.api.java.Messages impl;

    public GrpcMessagesController(String domain, Users usersService) {
        impl = new JavaMessages(domain, usersService);
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcMessagesGrpc.bindService(this);
    }

    @Override
    public void postMessage(Messages.PostMessageArgs request,
                            StreamObserver<Messages.PostMessageResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.postMessage(request.getPwd(),
                        GrpcMessage_to_Message(request.getMessage())),
                (mid) -> Messages.PostMessageResult.newBuilder().setMid(mid).build());
    }

    @Override
    public void getInboxMessage(Messages.GetInboxMessageArgs request,
                                StreamObserver<Messages.GrpcMessage> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getInboxMessage(request.getName(), request.getMid(),
                request.getPwd()),
                (message) -> Message_to_GrpcMessage(message));
    }

    @Override
    public void getAllInboxMessages(Messages.GetAllInboxMessagesArgs request,
                                    StreamObserver<Messages.GetAllInboxMessagesResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.getAllInboxMessages(request.getName(), request.getPwd()),
                (mids) -> Messages.GetAllInboxMessagesResult.newBuilder().addAllMids(mids).build());
    }

    @Override
    public void removeInboxMessage(Messages.RemoveInboxMessageArgs request,
                                   StreamObserver<Empty> responseObserver) {
        super.toGrpcResult(responseObserver, impl.removeInboxMessage(request.getName(), request.getMid(),
                request.getPwd()),
                (empty) -> Empty.newBuilder().build());
    }

    @Override
    public void deleteMessage(Messages.DeleteMessageArgs request,
                              StreamObserver<Empty> responseObserver) {
        super.toGrpcResult(responseObserver, impl.deleteMessage(request.getName(),
                request.getMid(), request.getPwd()),
                (empty) -> Empty.newBuilder().build());
    }

    @Override
    public void searchInbox(Messages.SearchInboxArgs request,
                            StreamObserver<Messages.SearchInboxResult> responseObserver) {
        super.toGrpcResult(responseObserver, impl.searchInbox(request.getName(), request.getPwd(),
                        request.getQuery()),
                (mids) -> Messages.SearchInboxResult.newBuilder().addAllMids(mids).build());
    }
}
