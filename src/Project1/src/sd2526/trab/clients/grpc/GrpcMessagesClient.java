package sd2526.trab.clients.grpc;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.grpc.Messages.*;
import sd2526.trab.server.grpc.DataModelAdaptor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GrpcMessagesClient extends GrpcClient implements Messages {

    private static Logger Log = Logger.getLogger(GrpcMessagesClient.class.getName());


    final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        super( serverURI, Log );

        stub = GrpcMessagesGrpc.newBlockingStub( channel );
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        PostMessageResult res = stub.postMessage(PostMessageArgs.newBuilder()
                .setPwd(pwd).setMessage(DataModelAdaptor.Message_to_GrpcMessage(msg)).build());

        return super.processResponse(() -> res.getMid());
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        Message message = DataModelAdaptor.GrpcMessage_to_Message(
                stub.getInboxMessage(GetInboxMessageArgs.newBuilder().setName(name)
                .setMid(mid).setPwd(pwd).build()));

         return super.processResponse( () -> message);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        GetAllInboxMessagesResult res = stub.getAllInboxMessages(
                GetAllInboxMessagesArgs.newBuilder().setName(name).setPwd(pwd).build());

        return super.processResponse( () -> {
            List<String> messagesList = new ArrayList<>();
            for(int i = 0; i < res.getMidsCount(); i++) {
                messagesList.add(res.getMids(i));
            }
            return messagesList;
        });
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {

        return super.processResponse( () -> { stub.removeInboxMessage(RemoveInboxMessageArgs.newBuilder()
                .setName(name).setMid(mid).setPwd(pwd).build());
                });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.processResponse( () -> { stub.deleteMessage(DeleteMessageArgs.newBuilder()
                .setName(name).setMid(mid).setPwd(pwd).build());
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        SearchInboxResult res = stub.searchInbox(SearchInboxArgs.newBuilder().setName(name)
                .setPwd(pwd).setQuery(query).build());

        return super.processResponse( () -> {
            List<String> messagesList = new ArrayList<>();
            for(int i = 0; i < res.getMidsCount(); i++) {
                messagesList.add(res.getMids(i));
            }
            return messagesList;
        });
    }
}
