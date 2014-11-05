package org.cobbzilla.wizard.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cobbzilla.util.http.HttpRequestBean;

import java.io.Closeable;
import java.io.IOException;

public class ProxyHttpClient implements Closeable {

    @Getter @Setter private CloseableHttpClient httpClient;
    @Getter @Setter private CredentialsProvider credsProvider = null;
    @Getter @Setter HttpClientContext localContext = null;

    public HttpResponse execute(HttpUriRequest request) throws IOException { return httpClient.execute(request, localContext); }

    @Override public void close() throws IOException { if (httpClient != null) httpClient.close(); }

    public ProxyHttpClient(HttpRequestBean requestBean) {
        if (requestBean.hasAuth()) {
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(requestBean.getHost(), requestBean.getPort()),
                    new UsernamePasswordCredentials(requestBean.getAuthUsername(), requestBean.getAuthPassword()));

            final AuthCache authCache = new BasicAuthCache();
            final AuthScheme authScheme = requestBean.getAuthType().newScheme();
            authCache.put(requestBean.getHttpHost(), authScheme);

            localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);
        }

        httpClient = requestBean.hasAuth()
                ? HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()
                : HttpClients.createDefault();
    }

}
