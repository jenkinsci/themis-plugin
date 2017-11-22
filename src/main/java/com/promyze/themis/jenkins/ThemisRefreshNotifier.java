package com.promyze.themis.jenkins;

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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.Symbol;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
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

        try (CloseableHttpClient httpClient = HttpClientUtils.getClient();
             CloseableHttpResponse response = refreshThemis(httpClient, instance)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                JSONObject result = new JSONObject(body);
                logger.println(Messages.projectRefreshed(result.get("dataDisplayed")));
            } else {
                fail(logger, Messages.refreshError(statusCode, body));
            }
        } catch (IOException | RuntimeException e) {
            fail(logger, Messages.refreshUnknownError(e.getMessage()));
        }
    }

    private CloseableHttpResponse refreshThemis(CloseableHttpClient client, ThemisInstance instance)
            throws IOException {
        String url = MessageFormat.format(REFRESH_URL_FORMAT, instance.getUrl(), projectKey);
        HttpGet request = new HttpGet(url);
        request.setHeader(THEMIS_API_KEY, instance.getApiKey());
        return client.execute(request);
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
