package com.promyze.themis.jenkins;

import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import com.promyze.themis.jenkins.action.ThemisAction;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for post build notifiers interacting with a Themis instance. It relies on an inner object extending
 * {@link ThemisAction} for performing the actual action.
 *
 * @param <T> The {@link ThemisAction} that will perform the post build action
 */
public abstract class BaseThemisNotifier<T extends ThemisAction> extends Notifier {

    /**
     * The inner action.
     */
    protected final T action;

    /**
     * Default constructor.
     *
     * @param action the action that should be performed when
     *               {@link #perform(AbstractBuild, Launcher, BuildListener)} is called
     */
    protected BaseThemisNotifier(T action) {
        this.action = action;
    }

    /**
     * @return the name of the Themis instance to use, as specified in the Jenkins global configuration
     */
    public String getInstanceName() {
        return action.getInstanceName();
    }

    /**
     * @return whether the build should fail if there is an error during the post build action
     */
    public boolean isFailBuild() {
        return action.isFailBuild();
    }

    /**
     * @param failBuild {@code true} if the build should fail
     */
    @DataBoundSetter
    public void setFailBuild(boolean failBuild) {
        action.setFailBuild(failBuild);
    }

    /**
     * Delegates to the inner action's {@code perform}.
     *
     * @return {@code true}
     * @see ThemisAction#perform(Run, FilePath, TaskListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        action.perform(build, build.getWorkspace(), listener);
        return true;
    }

    /**
     * Base descriptor for Themis notifiers.
     */
    public abstract static class BaseThemisNotifierDescriptor extends BuildStepDescriptor<Publisher> {

        @Inject
        private ThemisGlobalConfiguration globalConfiguration;

        public ListBoxModel doFillInstanceNameItems(@QueryParameter String instanceName) {
            return globalConfiguration.getInstances().stream()
                    .map(i -> new ListBoxModel.Option(i.getName(),
                                                      i.getName(),
                                                      i.getName().equals(instanceName)))
                    .collect(Collectors.toCollection(ListBoxModel::new));
        }

        public List<ThemisInstance> getInstances() {
            return globalConfiguration.getInstances();
        }

    }

}
