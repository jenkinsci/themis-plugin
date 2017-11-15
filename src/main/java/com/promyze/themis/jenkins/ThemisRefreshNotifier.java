package com.promyze.themis.jenkins;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sample notifier class
 */
public class ThemisRefreshNotifier extends Notifier {

    private static final String REFRESH_URL_FORMAT = "{0}/api/refreshProject/{1}";
    private static final String THEMIS_API_KEY = "themis-api-key";

    private String instanceName;
    private String projectKey;
    private boolean failBuild;

    @DataBoundConstructor
    public ThemisRefreshNotifier(String instanceName, String projectKey, boolean failBuild) {
        this.instanceName = instanceName;
        this.projectKey = projectKey;
        this.failBuild = failBuild;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public boolean getFailBuild() {
        return failBuild;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws AbortException {
        PrintStream logger = listener.getLogger();
        ThemisInstance instance = GlobalConfiguration.all().get(ThemisGlobalConfiguration.class)
                .getInstance(instanceName);
        if (instance == null) {
            return fail(logger, Messages.unknownInstance(instanceName));
        }

        try {
            HttpResponse<String> response = refreshThemis(instance);
            if (response.getStatus() == 200) {
                JsonNode node = new JsonNode(response.getBody());
                logger.println(Messages.projectRefreshed(node.getObject().get("dataDisplayed")));
            } else {
                return fail(logger, Messages.refreshError(response.getStatus(), response.getBody()));
            }
        } catch (UnirestException e) {
            return fail(logger, Messages.refreshUnknownError(e.getMessage()));
        }

        return true;
    }

    private HttpResponse<String> refreshThemis(ThemisInstance instance) throws UnirestException {
        String url = MessageFormat.format(REFRESH_URL_FORMAT, instance.getUrl(), projectKey);
        return Unirest.get(url)
                .header(THEMIS_API_KEY, instance.getApiKey())
                .asString();
    }

    private boolean fail(PrintStream logger, String message) throws AbortException {
        if (failBuild) {
            throw new AbortException(message);
        } else {
            logger.println(message);
            return true;
        }
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Publisher> {

        @Inject
        private ThemisGlobalConfiguration globalConfiguration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.refreshThemisProject();
        }

        public ListBoxModel doFillInstanceNameItems(@QueryParameter String instanceName) {
            return new ListBoxModel(globalConfiguration.getInstances().stream()
                                            .map(i -> new Option(i.getName(),
                                                                 i.getName(),
                                                                 i.getName().equals(instanceName)))
                                            .collect(Collectors.toList()));
        }

        public List<ThemisInstance> getInstances() {
            return globalConfiguration.getInstances();
        }

    }

}
