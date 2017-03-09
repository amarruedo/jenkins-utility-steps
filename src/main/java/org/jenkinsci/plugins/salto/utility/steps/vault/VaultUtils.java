package org.jenkinsci.plugins.salto.utility.steps.vault;

import hudson.AbortException;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.HttpMode;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.HttpRequest;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.MimeType;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.ResponseContentSupplier;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.util.HttpRequestNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VaultUtils {

    private final TaskListener listener;
    private String authToken;
    private final String vaultAddress;

    private static final String VAULT_API_VERSION = "v1";

    public String getAuthToken() {
        return authToken;
    }

    public VaultUtils(TaskListener listener, String vaultAddress){
        this.listener = listener;
        this.vaultAddress = vaultAddress;
    }

    public boolean Authenticate(String userName, String password) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();
        logger.println("********* Vault authentication ********");
        String url = "http://" + vaultAddress + "/" +VAULT_API_VERSION + "/auth/userpass/login/" + userName;

        HttpRequest httpRequest = new HttpRequest(url);
        httpRequest.setContentType(MimeType.APPLICATION_JSON);
        httpRequest.setHttpMode(HttpMode.POST);
        httpRequest.setRequestBody("{ \"password\": \"" + password + "\" }");

        ResponseContentSupplier httpResponse = httpRequest.performHttpRequest(listener);
        JSONObject obj = new JSONObject(httpResponse.getContent());

        if (obj.has("errors")){
            logger.println("Vault authentication failed: " + obj.getJSONArray("errors").getString(0));
            return false;
        }

        authToken = obj.getJSONObject("auth").getString("client_token");
        return true;
    }

    public Map<String, Object> GetSecrets(String secretPath) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();
        logger.println("********* Vault retrieveing secret ********");
        String url = "http://" + vaultAddress + "/" + VAULT_API_VERSION + "/secret" + secretPath;

        List<HttpRequestNameValuePair> customHeaders = new ArrayList<>();
        customHeaders.add(new HttpRequestNameValuePair("X-Vault-Token", authToken, true));
        HttpRequest httpRequest = new HttpRequest(url);
        httpRequest.setContentType(MimeType.APPLICATION_JSON);
        httpRequest.setHttpMode(HttpMode.GET);
        httpRequest.setCustomHeaders(customHeaders);

        ResponseContentSupplier httpResponse = httpRequest.performHttpRequest(listener);
        JSONObject obj = new JSONObject(httpResponse.getContent());

        if (obj.has("errors")){
            logger.println("Vault authentication failed: " + obj.getJSONArray("errors").getString(0));
            throw new AbortException("Fail: Vault error");
        }

        return obj.getJSONObject("data").toMap();
    }
}
