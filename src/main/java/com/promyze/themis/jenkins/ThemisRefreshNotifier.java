package com.promyze.themis.jenkins;

import com.promyze.themis.jenkins.action.ThemisAction;
import com.promyze.themis.jenkins.action.ThemisRefreshAction;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Extension class to refresh a Themis project. Implements both {@link hudson.tasks.Notifier} and
 * {@link SimpleBuildStep}, so it can work as a post build action in a freestyle project or as a build step in a
 * pipeline.
 * <p>
 * Pipeline can use the keyword {@code themisRefresh}:
 * </p>
 * <pre>themisRefresh(instanceName: 'Some instance', projectKey: 'key')</pre>
 */
public class ThemisRefreshNotifier extends BaseThemisNotifier<ThemisRefreshAction> implements SimpleBuildStep {

    /**
     * Default constructor.
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     * @param projectKey   the unique key of the project to refresh
     */
    @DataBoundConstructor
    public ThemisRefreshNotifier(String instanceName, String projectKey) {
        super(new ThemisRefreshAction(instanceName, projectKey));
    }

    /**
     * @return the unique key of the project to refresh
     */
    public String getProjectKey() {
        return action.getProjectKey();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Delegates to the inner action's {@code perform}.
     *
     * @param run          {@inheritDoc}
     * @param workspace    {@inheritDoc}
     * @param launcher     {@inheritDoc}
     * @param taskListener {@inheritDoc}
     * @see ThemisAction#perform(Run, FilePath, TaskListener)
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener taskListener) {
        action.perform(run, workspace, taskListener);
    }

    /**
     * Descriptor for {@link ThemisRefreshNotifier}.
     */
    @Symbol("themisRefresh")
    @Extension
    public static class Descriptor extends BaseThemisNotifierDescriptor {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.refreshThemisProject();
        }

    }

}
