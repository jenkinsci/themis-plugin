package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.Messages;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;

import java.io.Serializable;

/**
 * Base for classes that perform action on a Themis instance.
 */
public abstract class ThemisAction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String instanceName;
    private boolean failBuild;

    /**
     * Default constructor.
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     */
    protected ThemisAction(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * @return the name of the Themis instance to use, as specified in the Jenkins global configuration
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * @return whether the build should fail if there is an error during the post build action
     */
    public boolean isFailBuild() {
        return failBuild;
    }

    /**
     * Sets {@link #failBuild}.
     *
     * @param failBuild {@code true} if the build should fail
     */
    public void setFailBuild(boolean failBuild) {
        this.failBuild = failBuild;
    }

    /**
     * Performs the action.
     *
     * @param run       the current job execution
     * @param workspace the workspace of the current job
     * @param listener  the listener to send the output to
     */
    public void perform(Run<?, ?> run, FilePath workspace, TaskListener listener) {
        ThemisInstance instance = getInstance();
        if (instance == null) {
            fail(listener, Messages.unknownInstance(instanceName));
        } else {
            doPerform(instance, run, workspace, listener);
        }
    }

    private ThemisInstance getInstance() {
        return GlobalConfiguration.all().get(ThemisGlobalConfiguration.class).getInstance(instanceName);
    }

    void fail(TaskListener listener, String message) {
        fail(listener, message, null);
    }

    void fail(TaskListener listener, String message, Throwable t) {
        if (failBuild) {
            throw t == null ? new RuntimeException(message) : new RuntimeException(message, t);
        } else {
            listener.error(message);
            if (t != null) {
                listener.getLogger().println(t.getMessage());
            }
        }
    }

    abstract void doPerform(ThemisInstance instance, Run<?, ?> run, FilePath workspace, TaskListener listener);

}
