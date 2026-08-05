package sd2526.trab.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.clients.java.Clients;
import java.util.List;

//@Singleton
@Path(RestMessages.PATH)
public class RestGatewayMessagesResource extends RestResource implements RestMessages {

    private final String domain;

    public RestGatewayMessagesResource(String domain) {
        this.domain = domain;
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        return unwrapResultOrThrow(Clients.MessagesClient.get(domain).postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return unwrapResultOrThrow(Clients.MessagesClient.get(domain).getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        if (query == null || query.isEmpty()) {
            return unwrapResultOrThrow(Clients.MessagesClient.get(domain).getAllInboxMessages(name, pwd));
        } else {
            return unwrapResultOrThrow(Clients.MessagesClient.get(domain).searchInbox(name, pwd, query));
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        unwrapResultOrThrow(Clients.MessagesClient.get(domain).removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        unwrapResultOrThrow(Clients.MessagesClient.get(domain).deleteMessage(name, mid, pwd));
    }
}