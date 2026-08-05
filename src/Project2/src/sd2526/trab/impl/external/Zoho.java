package sd2526.trab.impl.external;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.ws.rs.WebApplicationException;
import sd2526.trab.api.Message;
import sd2526.trab.impl.external.zoho.ZohoServiceFactory;
import sd2526.trab.impl.external.zoho.ZohoTokenManager;
import sd2526.trab.impl.external.zoho.msgs.*;
import sd2526.trab.impl.rest.servers.ZohoRestMessagesServer;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.JSON;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

public class Zoho {

    private static final String ACCOUNTS = "/accounts";
    private static final String FOLDERS = "/folders/";
    private static final String MESSAGES = "/messages";
    private static final String SLASH = "/";

    static final String MAIL_API_BASE = "https://mail.zoho.eu/api";
    private static final String MAIL_API_ALL_MESSAGES = MESSAGES + "/view?includesent=true";


    static final String CLIENT_ID     = "1000.AQ7UO88NSD2D4GO6Q0V1RLZ7X4W74W";
    static final String CLIENT_SECRET = "4b201f42b1a8ab892abb2a531a754de2cdfe03ee6c";
    static final String REFRESH_TOKEN = "1000.c0b03d079dc9a1f7ffd4f872b75afb2c.09ed57ca6988bab45be64f1c4b0c2003";

    private static final String TRUE = "true";

    private static final String SEPARTING_MID = "_";
    private static final String SEPARTING_ACCOUNT = "]";
    private static final String MENOS_QUE = "<";
    private static final String MENOS_QUE_HTML = "&lt;";
    private static final String MAIS_QUE = ">";
    private static final String MAIS_QUE_HTML = "&gt;";
    private static final String APOSTROPHE = "'";
    private static final String APOSTROPHE_HTML = "&#39;";
    private static final String COMMA = ",";
    private static final String EMPTY = "";

    static final String AT_SIGN = "@";

    private static final int SUBCHAIN = 3;


    static final long MSG_CACHE_TTL = 60_000;

    final OAuth20Service service;
    final ZohoTokenManager tokenManager;

    static Zoho instance;

    private String argument;
    private String domain;
    private String accountId;
    private String accountName;
    private int offset;

    private final Cache<String, String> msgCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMillis(MSG_CACHE_TTL))
            .build();


    private Zoho() {
        argument = ZohoRestMessagesServer.getArg();
        setupHybridTrustManager();
        service = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
        tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
        checkDomainAndAccountId();
        offset = 1;
        if(argument.equals(TRUE)) {
            try {deleteAllMessages();}
            catch (Exception e) {}
        }
        else {
            try {refillCache();}
            catch (Exception e) {}
        }
    }

    synchronized public static Zoho getInstance() {
        if( instance == null ){
            instance = new Zoho();
        }

        return instance;
    }

    public List<ZohoAccount> getAccounts() throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if( response.isSuccessful() ) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoAccountReply.class).data();
                if (data == null || data.isEmpty()) return null;
                return data;
            }
            else {
                System.err.println( response.getCode() + SLASH + response.getBody() );
                return null;
            }
        }
    }

    public String sendEmail(String account, Message msg) throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.POST,
                MAIL_API_BASE + ACCOUNTS + SLASH + accountId + MESSAGES);
        request.addHeader("Content-Type", "application/json");

        String senderDomain = msg.senderAddress().split(AT_SIGN,2)[1];
        String mid = String.format("%s+%04d",senderDomain, (offset++));
        String destinations = String.join(COMMA, msg.getDestination());


        Map<String, String> emailBodyMap = java.util.Map.of(
                "fromAddress", accountName,
                "toAddress", destinations,
                "ccAddress", EMPTY,
                "bccAddress", EMPTY,
                "subject",  msg.getSender() + SEPARTING_ACCOUNT + account + SEPARTING_ACCOUNT +
                        SEPARTING_MID +  mid
                        + SEPARTING_MID + msg.getSubject(),
                "content", msg.getContents(),
                "askReceipt", EMPTY
        );
        String jsonEncoded = JSON.encode(emailBodyMap);
        request.setPayload(jsonEncoded);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if( response.isSuccessful() ) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoSendEmailReply.class).data();
                if (data == null) return null;

                var accessTokenGet = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
                OAuthRequest requestGet = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                    + SLASH + accountId + MAIL_API_ALL_MESSAGES);
                service.signRequest(accessTokenGet, requestGet);
                try (Response responseGet = service.execute(requestGet)) {
                    if( responseGet.isSuccessful() ) {
                        var bodyGet = responseGet.getBody();
                        var dataGet = JSON.decode(bodyGet, ZohoEmailReply.class).data();
                        for(ZohoEmail email : dataGet) {
                            String[] midCheck = email.subject().split(SEPARTING_MID, SUBCHAIN);
                            if(midCheck.length == SUBCHAIN && midCheck[1].trim().equals(mid)) {
                                msgCache.put(mid, email.messageId());
                            }
                        }
                    }
                }
                return mid;
            }
            else {
                System.err.println( response.getCode() + SLASH + response.getBody() );
                return null;
            }
        }
        catch( Exception e){
            System.err.println("Error");
            e.printStackTrace();
        }
        return null;
    }

    public Message getInbox(String name, String mid) throws Exception {
        String folderId = EMPTY;
        ZohoEmail email = null;

        var accessTokenGet = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
        OAuthRequest requestGet = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                + SLASH + accountId + MAIL_API_ALL_MESSAGES);
        service.signRequest(accessTokenGet, requestGet);
        try (Response responseGet = service.execute(requestGet)) {
            if( responseGet.isSuccessful() ) {
                var bodyGet = responseGet.getBody();
                var dataGet = JSON.decode(bodyGet, ZohoEmailReply.class).data();
                for(ZohoEmail emailCheck : dataGet) {
                    String[] midCheck = emailCheck.subject().split(SEPARTING_MID, SUBCHAIN);
                    if(midCheck.length == SUBCHAIN && midCheck[1].trim().equals(mid)) {
                        folderId = emailCheck.folderId();
                        email = emailCheck;
                        break;
                    }
                }

                if (folderId.isEmpty()) {
                    throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.NOT_FOUND);
                }

                var accessTokenGetContent = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
                OAuthRequest requestGetContent = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                        + SLASH + accountId + FOLDERS + folderId + MESSAGES + SLASH
                        + msgCache.getIfPresent(mid) + "/content");
                service.signRequest(accessTokenGetContent, requestGetContent);
                try (Response responseGetContent = service.execute(requestGetContent)) {
                    var bodyGetContent = responseGetContent.getBody();
                    ZohoGetEmail emailGetContent = JSON.decode(bodyGetContent, ZohoGetEmailReply.class).data();


                    Message msg = new Message();
                    msg.setId(mid);
                    msg.setSubject( email.subject().split(SEPARTING_MID, SUBCHAIN)[2].trim()
                            .replaceAll("(?i)<br\\s*/?>", "\n")
                            .replaceAll("<[^>]*>", EMPTY)
                            .replace(APOSTROPHE_HTML, APOSTROPHE)
                            .replace(MENOS_QUE_HTML, MENOS_QUE)
                            .replace(MAIS_QUE_HTML, MAIS_QUE));

                    String originalContent = emailGetContent.content().replaceAll("(?i)<br\\s*/?>", "\n")
                            .replaceAll("<[^>]*>", EMPTY).replace(APOSTROPHE_HTML, APOSTROPHE)
                            .replace(MENOS_QUE_HTML, MENOS_QUE).replace(MAIS_QUE_HTML, MAIS_QUE);

                    msg.setContents(originalContent);

                    Set<String> destsSet = Arrays.stream(email.toAddress().split(COMMA))
                            .map(String::trim)
                            .map(addr -> addr.replace(MENOS_QUE_HTML, EMPTY).
                                                replace(MAIS_QUE_HTML, EMPTY))
                            .collect(java.util.stream.Collectors.toSet());
                    msg.setDestination(destsSet);
                    msg.setSender( email.subject().split(SEPARTING_ACCOUNT, 2)[0].trim()
                            .replaceFirst(MENOS_QUE_HTML, MENOS_QUE)
                            .replaceFirst(MAIS_QUE_HTML, MAIS_QUE) );
                    return msg;
                }
                catch (Exception e){
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
            else {
                System.err.println("Error");
            }
        }

        throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.NOT_FOUND);
    }

    public List<String> getAllInbox(String name) throws Exception {
        for (int i = 0; i < 3; i++) {

            var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

            OAuthRequest request = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                    + SLASH + accountId + MAIL_API_ALL_MESSAGES);
            service.signRequest(accessToken, request);
            List<String> list = new ArrayList<>();
            try (Response response = service.execute(request)) {
                if( response.isSuccessful() ) {
                    var body = response.getBody();
                    var data = JSON.decode(body, ZohoEmailReply.class).data();
                    if (data != null && !data.isEmpty()) {

                        for(ZohoEmail email : data) {
                            String[] parts = email.subject().split(SEPARTING_ACCOUNT, SUBCHAIN);
                            if(parts.length == SUBCHAIN && parts[1].contains(name) ){
                                String value = email.subject().split(SEPARTING_MID, SUBCHAIN)[1].trim();
                                list.add(value);
                            }

                        }
                        if (!list.isEmpty()){
                            return list;
                        }
                    }
                }
            }
            catch( Exception e){
                System.err.println("Error");
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }

        return null;
    }

    public void deleteEmailFromInbox(String name, String mid)  throws Exception {
        String folderId = EMPTY;
        var accessTokenGet = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
        OAuthRequest requestGet = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                + SLASH + accountId + MAIL_API_ALL_MESSAGES);
        service.signRequest(accessTokenGet, requestGet);
        try (Response responseGet = service.execute(requestGet)) {
            if( responseGet.isSuccessful() ) {
                var bodyGet = responseGet.getBody();
                var dataGet = JSON.decode(bodyGet, ZohoEmailReply.class).data();
                for(ZohoEmail email : dataGet) {
                    String[] midCheck = email.subject().split(SEPARTING_MID, SUBCHAIN);
                    if(midCheck.length == SUBCHAIN && midCheck[1].trim().equals(mid)) {
                        folderId = email.folderId();
                    }
                }
            }
        }
        catch( Exception e){
            System.err.println("Error delete");
            e.printStackTrace();
        }

        if (folderId.isEmpty()) {
            throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.NOT_FOUND);
        }
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        String apiMid = msgCache.getIfPresent(mid);
        OAuthRequest requestDelete = new OAuthRequest(Verb.DELETE, (MAIL_API_BASE + ACCOUNTS)
                    + SLASH + accountId + FOLDERS + folderId + MESSAGES + SLASH + apiMid);
        service.signRequest(accessToken, requestDelete);
        try (Response responseDelete = service.execute(requestDelete)) {
            if (!responseDelete.isSuccessful()) {
                System.err.println("delete " + responseDelete.getCode() + SLASH + responseDelete.getBody() );
            }
        }
    }

    synchronized private void checkDomainAndAccountId() {
        if(domain == null) {
            domain = IP.domain();
            try{
                if(domain == null || domain.isEmpty()) {
                    ZohoAccount account = this.getAccounts().get(0);
                    domain = account.primaryEmailAddress().split(AT_SIGN)[1];
                }
            }
            catch (Exception e) {}
        }
        if(accountId == null) {
            try{
                    ZohoAccount account = this.getAccounts().get(0);
                    accountId = account.accountId();
                    accountName = account.primaryEmailAddress();
            }
            catch (Exception e) {}
        }
    }

    public void remotePostMessage(Message m) throws Exception {
        List<String> list = m.getDestination().stream().
                filter( (address) -> address.endsWith( AT_SIGN + domain ) ).toList();
        for(String account : list) {
            try {
                this.sendEmail(account, m);
            }
            catch (Exception e){
                throw e;
            }
        }
    }

    private void deleteAllMessages() throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        OAuthRequest requestList = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                + SLASH + accountId + MAIL_API_ALL_MESSAGES);
        service.signRequest(accessToken, requestList);
        try (Response responseList = service.execute(requestList)) {
            if (!responseList.isSuccessful()){
                return;
            }

            var data = JSON.decode(responseList.getBody(), ZohoEmailReply.class).data();
            if (data == null || data.isEmpty()) return;
            Map<String, List<String>> messagesByFolder = new HashMap<>();
            for (ZohoEmail email : data) {
                if(! (email.subject().split(SEPARTING_MID, SUBCHAIN).length < 2)) {
                    messagesByFolder.computeIfAbsent(email.folderId(), k -> new ArrayList<>()).add(email.messageId());
                }
            }

            for (Map.Entry<String, List<String>> entry : messagesByFolder.entrySet()) {
                String folderId = entry.getKey();
                String idsInFolder = String.join(COMMA, entry.getValue());
                var accessTokenDelete = new OAuth2AccessToken(tokenManager.getValidAccessToken());
                OAuthRequest requestDelete = new OAuthRequest(Verb.DELETE, (MAIL_API_BASE + ACCOUNTS)
                        + SLASH + accountId + FOLDERS + folderId + MESSAGES + SLASH + idsInFolder);

                service.signRequest(accessTokenDelete, requestDelete);
                service.execute(requestDelete);
            }
        }
        catch (Exception e) {
            System.err.println("Error deleting ALL");
            e.printStackTrace();
        }
    }

    private void refillCache() throws Exception{
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.GET, (MAIL_API_BASE + ACCOUNTS)
                + SLASH + accountId + MAIL_API_ALL_MESSAGES);
        service.signRequest(accessToken, request);
        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoEmailReply.class).data();

                if (data != null && !data.isEmpty()) {

                    for (ZohoEmail email : data) {
                        String[] parts = email.subject().split(SEPARTING_ACCOUNT, SUBCHAIN);
                        if (parts.length == SUBCHAIN) {
                            String value = email.subject().split(SEPARTING_MID, SUBCHAIN)[1].trim();
                            if (msgCache.getIfPresent(value) == null) {
                                msgCache.put(value, email.messageId());
                                offset++;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void setupHybridTrustManager() {
        try {
            TrustManagerFactory tmfSystem = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfSystem.init((KeyStore) null);
            X509TrustManager systemTM = getX509TrustManager(tmfSystem);

            File cacertsFile = new File(System.getProperty("java.home") + "/lib/security/cacerts");
            KeyStore cacertsKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream in = new FileInputStream(cacertsFile)) {
                cacertsKeyStore.load(in, "changeit".toCharArray());
            }
            TrustManagerFactory tmfDefault = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfDefault.init(cacertsKeyStore);
            X509TrustManager defaultTM = getX509TrustManager(tmfDefault);

            X509TrustManager hybridTM = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try { systemTM.checkClientTrusted(chain, authType); }
                    catch (CertificateException e) { defaultTM.checkClientTrusted(chain, authType); }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        systemTM.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        defaultTM.checkServerTrusted(chain, authType);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTM.getAcceptedIssuers();
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{hybridTM}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static X509TrustManager getX509TrustManager(TrustManagerFactory tmf) {
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) return (X509TrustManager) tm;
        }
        return null;
    }

}
