package sd2526.trab.server.grpc;

import java.util.HashSet;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Messages.GrpcMessage;
import sd2526.trab.api.grpc.Users;

public class DataModelAdaptor {

    public static Message GrpcMessage_to_Message(GrpcMessage from) {
        System.out.println("PASSED" + from.getCreationTime());
        return new Message(
                from.getId().isEmpty() ? null : from.getId(),
                from.getSender(),
                new HashSet<>(from.getDestinationList()),
                from.getSubject(),
                from.getContents(),
                from.getCreationTime());
    }

    public static GrpcMessage Message_to_GrpcMessage(Message from) {
        GrpcMessage.Builder b = GrpcMessage.newBuilder();

        if(from.getId() != null)
            b.setId(from.getId());

        if(from.getSender() != null)
            b.setSender(from.getSender());

        if(from.getContents() != null)
            b.setContents(from.getContents());

        if(from.getSubject() != null)
            b.setSubject(from.getSubject());

        b.setCreationTime(from.getCreationTime());

        if(from.getDestination() != null)
            b.addAllDestination(from.getDestination());

        return b.build();
    }

    public static User GrpcUser_to_User(Users.GrpcUser from) {
        return new User(
                from.getName().isEmpty() ? null : from.getName(),
                from.getPwd().isEmpty() ? null : from.getPwd(),
                from.getDisplayName().isEmpty() ? null : from.getDisplayName(),
                from.getDomain().isEmpty() ? null : from.getDomain()
        );
    }

    public static Users.GrpcUser User_to_GrpcUser(User from) {
        Users.GrpcUser.Builder b = Users.GrpcUser.newBuilder();

        if(from.getName() != null)
            b.setName(from.getName());

        if(from.getPwd() != null)
            b.setPwd(from.getPwd());

        if(from.getDisplayName() != null)
            b.setDisplayName(from.getDisplayName());

        if(from.getDomain() != null)
            b.setDomain(from.getDomain());

        return b.build();
    }
}