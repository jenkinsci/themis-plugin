package com.promyze.themis.jenkins;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;

import java.util.List;
import java.util.regex.Pattern;

public class HttpClientUtils {

    private HttpClientUtils() {
        // private constructor for utility class
    }

    public static CloseableHttpClient getClient() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.proxy != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            clientBuilder.setRoutePlanner(new ProxyRoutePlanner(proxy));
            if (proxy.getUserName() != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(proxy.name, proxy.port),
                                                   new UsernamePasswordCredentials(proxy.getUserName(),
                                                                                   proxy.getPassword()));
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            }
        }
        return clientBuilder.build();
    }

    private static final class ProxyRoutePlanner extends DefaultRoutePlanner {

        private final HttpHost proxy;
        private final List<Pattern> noProxyHostPatterns;

        public ProxyRoutePlanner(ProxyConfiguration proxyConfiguration) {
            super(null);
            this.proxy = new HttpHost(proxyConfiguration.name, proxyConfiguration.port);
            this.noProxyHostPatterns = proxyConfiguration.getNoProxyHostPatterns();
        }

        @Override
        protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) {
            String hostName = target.getHostName();
            return noProxyHostPatterns.stream().anyMatch(p -> p.matcher(hostName).matches()) ? null : proxy;
        }
    }

}
