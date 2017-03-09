package org.jenkinsci.plugins.salto.utility.steps.vault.http.util;

import org.apache.http.entity.ContentType;
import org.jenkinsci.plugins.salto.utility.steps.vault.http.HttpMode;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestAction {

    private final URL url;
    private final HttpMode mode;
    private final String requestBody;
    private final List<HttpRequestNameValuePair> params;
    private final List<HttpRequestNameValuePair> headers;
    private ContentType contentType;

    public RequestAction(URL url, HttpMode mode, String requestBody, List<HttpRequestNameValuePair> params) {
        this(url, mode, requestBody, params, null);
    }

    public RequestAction(URL url, HttpMode mode, String requestBody, List<HttpRequestNameValuePair> params, List<HttpRequestNameValuePair> headers) {
        this(url, mode, requestBody, params, headers, ContentType.DEFAULT_TEXT);
    }

    public RequestAction(URL url, HttpMode mode, String requestBody, List<HttpRequestNameValuePair> params, List<HttpRequestNameValuePair> headers, ContentType contentType) {
        this.url = url;
        this.mode = mode;
        this.requestBody = requestBody;
        this.params = params == null ? new ArrayList<HttpRequestNameValuePair>() : params;
        this.headers = headers  == null ? new ArrayList<HttpRequestNameValuePair>() : headers;
        this.contentType = contentType;
    }

    public URL getUrl() {
        return url;
    }

    public HttpMode getMode() {
        return mode;
    }

    public List<HttpRequestNameValuePair> getParams() {
        return Collections.unmodifiableList(params);
    }

    public List<HttpRequestNameValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public String getRequestBody() {
        return requestBody;
    }

    public ContentType getContentType() {
        return contentType;
    }
}
