package com.promyze.themis.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Global configuration for the Themis Jenkins plugin
 */
@Extension
public class ThemisGlobalConfiguration extends GlobalConfiguration {

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
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
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

            public static final String VALID_URL_PATTERN = "^https?://.+";

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

            private void checkNotNullOrEmpty(String value, String message) throws FormValidation {
                if (value == null || value.length() == 0) {
                    throw FormValidation.error(message);
                }
            }

        }

    }

}