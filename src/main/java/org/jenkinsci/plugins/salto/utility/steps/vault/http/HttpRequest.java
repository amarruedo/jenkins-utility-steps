package org.jenkinsci.plugins.salto.utility.steps.vault.http;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import hudson.*;

import hudson.model.*;
import hudson.util.VariableResolver;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;

import org.jenkinsci.plugins.salto.utility.steps.vault.http.util.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;


public class HttpRequest {

    private @Nonnull String url;
    private HttpMode httpMode                 = HttpMode.GET;
    private Boolean passBuildParameters       = false;
    private String validResponseCodes         = "100:399";
    private String validResponseContent       = "";
    private MimeType acceptType               = MimeType.NOT_SET;
    private MimeType contentType              = MimeType.NOT_SET;
    private String outputFile                 = "";
    private Integer timeout                   = 0;
    private Boolean consoleLogResponseBody    = false;
    private String authentication             = "";
    private String requestBody                = "";
    private List<HttpRequestNameValuePair> customHeaders = Collections.<HttpRequestNameValuePair>emptyList();

    public HttpRequest(@Nonnull String url) {
        this.url = url;
    }

    public void setHttpMode(HttpMode httpMode) {
        this.httpMode = httpMode;
    }

    public void setPassBuildParameters(Boolean passBuildParameters) {
        this.passBuildParameters = passBuildParameters;
    }

    public void setValidResponseCodes(String validResponseCodes) {
        this.validResponseCodes = validResponseCodes;
    }

    public void setValidResponseContent(String validResponseContent) {
        this.validResponseContent = validResponseContent;
    }

    public void setAcceptType(MimeType acceptType) {
        this.acceptType = acceptType;
    }

    public void setContentType(MimeType contentType) {
        this.contentType = contentType;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void setConsoleLogResponseBody(Boolean consoleLogResponseBody) {
        this.consoleLogResponseBody = consoleLogResponseBody;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void setCustomHeaders(List<HttpRequestNameValuePair> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public @Nonnull String getUrl() {
        return url;
    }

    public HttpMode getHttpMode() {
        return httpMode;
    }

    public String getAuthentication() {
        return authentication;
    }

    public MimeType getContentType() {
        return contentType;
    }

    public MimeType getAcceptType() {
        return acceptType;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public Boolean getConsoleLogResponseBody() {
        return consoleLogResponseBody;
    }

    public Boolean getPassBuildParameters() {
        return passBuildParameters;
    }

    public List<HttpRequestNameValuePair> getCustomHeaders() {
        return customHeaders;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public @Nonnull String getValidResponseCodes() {
        return validResponseCodes;
    }

    public String getValidResponseContent() {
        return validResponseContent;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public ResponseContentSupplier performHttpRequest(TaskListener listener)
    throws InterruptedException, IOException
    {
        List<HttpRequestNameValuePair> params = Collections.emptyList();
        List<HttpRequestNameValuePair> headers = new ArrayList<>();
        if (contentType != MimeType.NOT_SET) {
            headers.add(new HttpRequestNameValuePair("Content-type", contentType.getContentType().toString()));
        }
        if (acceptType != MimeType.NOT_SET) {
            headers.add(new HttpRequestNameValuePair("Accept", acceptType.getValue()));
        }
        for (HttpRequestNameValuePair header : customHeaders) {
            headers.add(new HttpRequestNameValuePair(header.getName(), header.getValue(), header.getMaskValue()));
        }

        RequestAction requestAction = new RequestAction(new URL(url), httpMode, requestBody, params, headers, contentType.getContentType());

        return performHttpRequest(listener, requestAction);
    }

    public ResponseContentSupplier performHttpRequest(TaskListener listener, RequestAction requestAction)
    throws InterruptedException, IOException
    {
        final PrintStream logger = listener.getLogger();
        logger.println("HttpMode: " + requestAction.getMode());
        logger.println(String.format("URL: %s", requestAction.getUrl()));
        for (HttpRequestNameValuePair header : requestAction.getHeaders()) {
            if (header.getMaskValue() || header.getName().equalsIgnoreCase("Authorization")) {
              logger.println(header.getName() + ": *****");
            } else {
              logger.println(header.getName() + ": " + header.getValue());
            }
        }

        //HttpClient client = HttpClientBuilder.create().build();
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        HttpClientUtil clientUtil = new HttpClientUtil();
        HttpRequestBase httpRequestBase = clientUtil.createRequestBase(requestAction);

        HttpContext context = new BasicHttpContext();

        ResponseContentSupplier responseContentSupplier;
        try {
            final HttpResponse response = clientUtil.execute(httpclient, context, httpRequestBase, logger, timeout);
            // The HttpEntity is consumed by the ResponseContentSupplier
            responseContentSupplier = new ResponseContentSupplier(response);
        } catch (UnknownHostException uhe) {
            logger.println("Treating UnknownHostException(" + uhe.getMessage() + ") as 404 Not Found");
            responseContentSupplier = new ResponseContentSupplier("UnknownHostException as 404 Not Found", 404);
        } catch (SocketTimeoutException | ConnectException ce) {
            logger.println("Treating " + ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout");
            responseContentSupplier = new ResponseContentSupplier(ce.getClass() + "(" + ce.getMessage() + ") as 408 Request Timeout", 408);
        }

        if (consoleLogResponseBody) {
            logger.println("Response: \n" + responseContentSupplier.getContent());
        }

        responseCodeIsValid(responseContentSupplier, logger);
        contentIsValid(responseContentSupplier, logger);

        return responseContentSupplier;
    }

    private void contentIsValid(ResponseContentSupplier responseContentSupplier, PrintStream logger)
    throws AbortException
    {
        if (Strings.isNullOrEmpty(validResponseContent)) {
            return;
        }

        String response = responseContentSupplier.getContent();
        if (!response.contains(validResponseContent)) {
            throw new AbortException("Fail: Response with length " + response.length() + " doesn't contain '" + validResponseContent + "'");
        }
        return;
    }

    private void responseCodeIsValid(ResponseContentSupplier response, PrintStream logger)
    throws AbortException
    {
        List<Range<Integer>> ranges = parseToRange(validResponseCodes);
        for (Range<Integer> range : ranges) {
            if (range.contains(response.getStatus())) {
                logger.println("Success code from " + range);
                return;
            }
        }
        throw new AbortException("Fail: the returned code " + response.getStatus()+" is not in the accepted range: "+ranges);
    }

    private List<Range<Integer>> parseToRange(String value) {
        List<Range<Integer>> validRanges = new ArrayList<Range<Integer>>();

        String[] codes = value.split(",");
        for (String code : codes) {
            String[] fromTo = code.trim().split(":");
            checkArgument(fromTo.length <= 2, "Code %s should be an interval from:to or a single value", code);

            Integer from;
            try {
                from = Integer.parseInt(fromTo[0]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid number "+fromTo[0]);
            }

            Integer to = from;
            if (fromTo.length != 1) {
                try {
                    to = Integer.parseInt(fromTo[1]);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid number "+fromTo[1]);
                }
            }

            checkArgument(from <= to, "Interval %s should be FROM less than TO", code);
            validRanges.add(Ranges.closed(from, to));
        }

        return validRanges;
    }

    private void logResponseToFile(FilePath workspace, PrintStream logger, ResponseContentSupplier responseContentSupplier) throws IOException, InterruptedException {

        FilePath outputFilePath = getOutputFilePath(workspace, logger);

        if (outputFilePath != null && responseContentSupplier.getContent() != null) {
            OutputStreamWriter write = null;
            try {
                write = new OutputStreamWriter(outputFilePath.write(), Charset.forName("UTF-8"));
                write.write(responseContentSupplier.getContent());
            } finally {
                if (write != null) {
                    write.close();
                }
            }
        }
    }

    private FilePath getOutputFilePath(FilePath workspace, PrintStream logger) {
        if (outputFile != null && !outputFile.isEmpty()) {
            return workspace.child(outputFile);
        }
        return null;
    }

    private List<HttpRequestNameValuePair> createParameters(
            AbstractBuild<?, ?> build, PrintStream logger,
            EnvVars envVars) {
        if (passBuildParameters == null || !passBuildParameters) {
            return Collections.emptyList();
        }

        if (!envVars.isEmpty()) {
            logger.println("Parameters: ");
        }

        final VariableResolver<String> vars = build.getBuildVariableResolver();

        List<HttpRequestNameValuePair> l = new ArrayList<HttpRequestNameValuePair>();
        for (Map.Entry<String, String> entry : build.getBuildVariables().entrySet()) {
            String value = evaluate(entry.getValue(), vars, envVars);
            logger.println("  " + entry.getKey() + " = " + value);

            l.add(new HttpRequestNameValuePair(entry.getKey(), value));
        }

        return l;
    }

    private String evaluate(String value, VariableResolver<String> vars, Map<String, String> env) {
        return Util.replaceMacro(Util.replaceMacro(value, vars), env);
    }
}
