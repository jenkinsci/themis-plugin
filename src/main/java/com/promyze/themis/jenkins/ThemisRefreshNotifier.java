package com.promyze.themis.jenkins;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sample notifier class
 */
public class ThemisRefreshNotifier extends Notifier implements SimpleBuildStep {

    private static final String REFRESH_URL_FORMAT = "{0}/api/refreshProject/{1}";
    private static final String THEMIS_API_KEY = "themis-api-key";

    private final String instanceName;
    private final String projectKey;
    private boolean failBuild;

    @DataBoundConstructor
    public ThemisRefreshNotifier(String instanceName, String projectKey) {
        this.instanceName = instanceName;
        this.projectKey = projectKey;
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

    @DataBoundSetter
    public void setFailBuild(boolean failBuild) {
        this.failBuild = failBuild;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws AbortException {
        perform(listener.getLogger());
        return true;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath filePath,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener taskListener)
            throws AbortException {
        perform(taskListener.getLogger());
    }

    private void perform(PrintStream logger) throws AbortException {
        ThemisInstance instance = GlobalConfiguration.all().get(ThemisGlobalConfiguration.class)
                .getInstance(instanceName);
        if (instance == null) {
            fail(logger, Messages.unknownInstance(instanceName));
            return;
        }

        try {
            HttpResponse<String> response = refreshThemis(instance);
            if (response.getStatus() == 200) {
                JsonNode node = new JsonNode(response.getBody());
                logger.println(Messages.projectRefreshed(node.getObject().get("dataDisplayed")));
            } else {
                fail(logger, Messages.refreshError(response.getStatus(), response.getBody()));
            }
        } catch (UnirestException | RuntimeException e) {
            fail(logger, Messages.refreshUnknownError(e.getMessage()));
        }
    }

    private HttpResponse<String> refreshThemis(ThemisInstance instance) throws UnirestException {
        String url = MessageFormat.format(REFRESH_URL_FORMAT, instance.getUrl(), projectKey);
        return Unirest.get(url)
                .header(THEMIS_API_KEY, instance.getApiKey())
                .asString();
    }

    private void fail(PrintStream logger, String message) throws AbortException {
        if (failBuild) {
            throw new AbortException(message);
        } else {
            logger.println(message);
        }
    }

    @Symbol("themisRefresh")
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
