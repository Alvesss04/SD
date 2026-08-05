package sd2526.trab.server.rest;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.api.java.Messages;

import java.util.List;
import java.util.logging.Logger;

@Singleton
public class RestMessageResource extends RestResource implements RestMessages {

    private static final Logger Log = Logger.getLogger(RestMessageResource.class.getName());

    private final Messages msg;

    public RestMessageResource(Messages logic) {
        this.msg = logic;
    }

    @Override
    public String postMessage(String pwd, Message message) {
        Log.info("postMessage : pwd=" + pwd + " msg=" + message);
        return unwrapResultOrThrow(msg.postMessage(pwd, message));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        Log.info("getMessage : name=" + name + " mid=" + mid);
        return unwrapResultOrThrow(msg.getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        Log.info("getMessages : name=" + name + " query='" + query + "'");
        if (query == null || query.isEmpty()) {
            return unwrapResultOrThrow(msg.getAllInboxMessages(name, pwd));
        } else {
            return unwrapResultOrThrow(msg.searchInbox(name, pwd, query));
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        Log.info("removeFromUserInbox : name=" + name + " mid=" + mid);
        unwrapResultOrThrow(msg.removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        Log.info("deleteMessage : name=" + name + " mid=" + mid);
        unwrapResultOrThrow(msg.deleteMessage(name, mid, pwd));
    }
}