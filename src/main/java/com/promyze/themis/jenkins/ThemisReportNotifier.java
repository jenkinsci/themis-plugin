package com.promyze.themis.jenkins;

import com.promyze.themis.jenkins.action.ThemisReportAction;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepMonitor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notifier that sends report files (e.g., test coverage reports) to a Themis instance in a post-build action.
 */
public class ThemisReportNotifier extends BaseThemisNotifier<ThemisReportAction> {

    /**
     * Default constructor.
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     * @param sourceKey    the unique key of the source for which to send reports
     */
    @DataBoundConstructor
    public ThemisReportNotifier(String instanceName, String sourceKey) {
        super(new ThemisReportAction(instanceName, sourceKey));
    }

    /**
     * @return the unique key of the source for which to send reports
     */
    public String getSourceKey() {
        return action.getSourceKey();
    }

    /**
     * @return the report files
     */
    public List<ReportFile> getReportFiles() {
        return action.getReports().entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(r -> new ReportFile(e.getKey(), r)))
                .collect(Collectors.toList());
    }

    /**
     * Sets the report files.
     *
     * @param reportFiles a list of report files
     */
    @DataBoundSetter
    public void setReportFiles(List<ReportFile> reportFiles) {
        action.getReports().clear();
        reportFiles.forEach(action::addReportFile);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for {@link ThemisRefreshNotifier}.
     */
    @Extension
    public static class ThemisReportDescriptor extends BaseThemisNotifierDescriptor {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.sendReportFiles();
        }
    }

}
