package com.promyze.themis.jenkins;

import com.promyze.themis.jenkins.action.ThemisReportAction;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pipeline step that sends report files (e.g., test coverage reports) to a Themis instance in a post-build action.
 * <p>
 * To add in a pipeline, use the keyword {@code themisReport}:
 * </p>
 * <pre>themisReport(instanceName: 'Some instance', sourceKey: 'key', reports: [[type: 'type', path: 'path']])</pre>
 */
public class ThemisReportStep extends Step {

    private final ThemisReportAction action;

    /**
     * Default constructor
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     * @param sourceKey    the unique key of the source for which to send reports
     */
    @DataBoundConstructor
    public ThemisReportStep(String instanceName, String sourceKey) {
        this.action = new ThemisReportAction(instanceName, sourceKey);
    }

    /**
     * @return the reports as a list of map that contains the report type with {@link ThemisReportAction#TYPE_KEY}
     * as key and the report path with {@link ThemisReportAction#PATH_KEY} as key
     */
    public List<Map<String, String>> getReports() {
        return action.getReports().entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(p -> getMap(e.getKey(), p)))
                .collect(Collectors.toList());
    }

    private Map<String, String> getMap(String type, String path) {
        Map<String, String> map = new HashMap<>();
        map.put(ThemisReportAction.TYPE_KEY, type);
        map.put(ThemisReportAction.PATH_KEY, path);
        return map;
    }

    /**
     * Sets the reports
     *
     * @param reports the reports as a list of map that contains the report type with
     *                {@link ThemisReportAction#TYPE_KEY} as key and the report path with
     *                {@link ThemisReportAction#PATH_KEY} as key
     */
    @DataBoundSetter
    public void setReports(List<Map<String, String>> reports) {
        action.getReports().clear();
        reports.forEach(action::addReport);
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

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, action);
    }

    private static class Execution extends SynchronousStepExecution {

        private static final long serialVersionUID = 1L;

        private final ThemisReportAction action;

        private Execution(StepContext context, ThemisReportAction action) {
            super(context);
            this.action = action;
        }

        @Override
        protected Object run() throws Exception {
            action.setEnvVars(getContext().get(EnvVars.class));
            Run<?, ?> run = getContext().get(Run.class);
            FilePath workspace = getContext().get(FilePath.class);
            TaskListener listener = getContext().get(TaskListener.class);
            action.perform(run, workspace, listener);
            return null;
        }
    }

    /**
     * Descriptor for {@link ThemisReportStep}.
     */
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(Run.class, FilePath.class, TaskListener.class, EnvVars.class));
        }

        @Override
        public String getFunctionName() {
            return "themisReport";
        }

        @Override
        public String getDisplayName() {
            return Messages.sendReportFiles();
        }

    }

}
