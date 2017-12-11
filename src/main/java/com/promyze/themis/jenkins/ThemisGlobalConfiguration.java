package com.promyze.themis.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.promyze.themis.jenkins.FormValidationUtils.checkNotNullOrEmpty;

/**
 * Global configuration for the Themis Jenkins plugin
 */
@Extension
public class ThemisGlobalConfiguration extends GlobalConfiguration {

    public static final String THEMIS_API_KEY = "themis-api-key";

    private volatile List<ThemisInstance> instances = new ArrayList<>();

    public ThemisGlobalConfiguration() {
        load();
    }

    public List<ThemisInstance> getInstances() {
        return instances;
    }

    public ThemisInstance getInstance(String name) {
        Objects.requireNonNull(name, "Parameter name must not be null");
        return instances.stream().filter(i -> name.equals(i.getName())).findAny().orElse(null);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        this.instances = req.bindJSONToList(ThemisInstance.class, json.get("instances"));
        save();
        return true;
    }

    /**
     * A simple class to contain the information about a Themis instance.
     */
    public static final class ThemisInstance extends AbstractDescribableImpl<ThemisInstance> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final String url;
        private final String apiKey;

        @DataBoundConstructor
        public ThemisInstance(String name, String url, String apiKey) {
            this.name = name;
            this.url = url;
            this.apiKey = apiKey;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getApiKey() {
            return apiKey;
        }

        @Extension
        public static class ThemisInstanceDescriptor extends Descriptor<ThemisInstance> {

            private static final String VALID_URL_PATTERN = "^https?://.+";
            private static final String TEST_URL_FORMAT = "{0}/api/testConnection";

            @Override
            public String getDisplayName() {
                return "";
            }

            public FormValidation doCheckName(@QueryParameter String name) {
                try {
                    checkNotNullOrEmpty(name, Messages.nameIsRequired());
                    return FormValidation.ok();
                } catch (FormValidation formValidation) {
                    return formValidation;
                }
            }

            public FormValidation doCheckUrl(@QueryParameter String url) {
                try {
                    checkNotNullOrEmpty(url, Messages.urlIsRequired());
                    return !url.matches(VALID_URL_PATTERN)
                            ? FormValidation.error(Messages.invalidUrl())
                            : FormValidation.ok();
                } catch (FormValidation formValidation) {
                    return formValidation;
                }
            }

            public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
                try {
                    checkNotNullOrEmpty(apiKey, Messages.apiKeyIsRequired());
                    return FormValidation.ok();
                } catch (FormValidation formValidation) {
                    return formValidation;
                }
            }

            public FormValidation doTestConnection(@QueryParameter String url, @QueryParameter String apiKey) {
                try (CloseableHttpClient client = HttpClientUtils.getClient();
                     CloseableHttpResponse response = client.execute(getTestRequest(url, apiKey))) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    switch (statusCode) {
                        case 200:
                        case 404: // when testing on an old Themis instance
                            return FormValidation.ok(Messages.testOk());
                        case 401:
                        case 403:
                        case 500: // when testing on an old Themis instance
                            return FormValidation.error(Messages.authenticationError());
                        default:
                            return FormValidation.error(Messages.validationFailure(statusCode));
                    }
                } catch (HttpHostConnectException e) {
                    return FormValidation.error(Messages.noConnection());
                } catch (IOException e) {
                    return FormValidation.error(e, Messages.validationError());
                }
            }

            private HttpGet getTestRequest(String url, String apiKey) {
                HttpGet request = new HttpGet(MessageFormat.format(TEST_URL_FORMAT, url));
                request.setHeader(THEMIS_API_KEY, apiKey);
                return request;
            }

        }

    }

}