package sd2526.trab.impl.external;

import jakarta.ws.rs.WebApplicationException;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.api.rest.RestAdminMessages;
import sd2526.trab.impl.external.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.rest.servers.RestResource;

import java.util.List;

public class ZohoRestMessagesResource extends RestResource implements RestMessages, RestAdminMessages {

    public ZohoRestMessagesResource() {
    }


    @Override
    public String postMessage(String pwd, Message msg) {
        try{
            String sender = msg.senderAddress().split("@")[0];
            return Zoho.getInstance().sendEmail(sender, msg);
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        try{
            return Zoho.getInstance().getInbox(name, mid);
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.NOT_FOUND);
        }
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        try{
            return Zoho.getInstance().getAllInbox(name);
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        try{
            Zoho.getInstance().deleteEmailFromInbox(name, mid);;
        }

        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            return;
        }
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        try {
            List<ZohoAccount> accounts = Zoho.getInstance().getAccounts();
            for(ZohoAccount account : accounts) {
                Zoho.getInstance().deleteEmailFromInbox(account.accountName(), mid);
            }
        }
        catch (WebApplicationException e) {
            System.out.println( e);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void remotePostMessage(Message m) {
        try{
            Zoho.getInstance().remotePostMessage(m);
        }

        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            return;
        }
    }

    @Override
    public void remoteDeleteMessage(String mid) {

    }

    @Override
    public void remoteDeleteUserInbox(String name) {

    }
}
