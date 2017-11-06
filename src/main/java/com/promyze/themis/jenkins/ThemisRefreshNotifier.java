package com.promyze.themis.jenkins;

import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
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
import java.util.stream.Collectors;

/**
 * Sample notifier class
 */
public class ThemisRefreshNotifier extends Notifier {

    private String instanceName;
    private String projectKey;

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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        ThemisInstance instance = GlobalConfiguration.all().get(ThemisGlobalConfiguration.class)
                .getInstance(instanceName);
        if (instance == null) {
            throw new RuntimeException(Messages.unknownInstance(instanceName));
        }
        // TODO actually refresh Themis
        listener.getLogger().println(String.format("Refreshing Themis: %s?apiKey=%s&projectKey=%s",
                                                   instance.getUrl(), instance.getApiKey(), projectKey));
        return true;
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

    }

}