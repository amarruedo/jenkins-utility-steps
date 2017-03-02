package org.jenkinsci.plugins.salto.utility.steps.vault.http.util;

import org.apache.http.NameValuePair;


public class HttpRequestNameValuePair
        implements NameValuePair {

    private final String name;
    private final String value;
    private final boolean maskValue;

    public HttpRequestNameValuePair(String name, String value, boolean maskValue) {
        this.name = name;
        this.value = value;
        this.maskValue = maskValue;
    }

    public HttpRequestNameValuePair(String name, String value) {
        this(name, value, false);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean getMaskValue() {
        return maskValue;
    }

}
