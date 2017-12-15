package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.ThemisGlobalConfiguration;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

public class ThemisActionTest extends BaseThemisActionTest<ThemisAction> {

    @Before
    public void setupAction() {
        action = Mockito.spy(new ThemisActionImpl(INSTANCE_NAME));
    }

    @Test
    public void testGetInstanceName() {
        assertThat(action.getInstanceName()).isEqualTo(INSTANCE_NAME);
    }

    @Test
    public void testIsFailBuildDefault() {
        assertThat(action.isFailBuild()).isFalse();
    }

    @Test
    public void testIsFailBuildChanged() {
        action.setFailBuild(true);

        assertThat(action.isFailBuild()).isTrue();
    }

    @Test
    public void testPerformNominal() {
        action.perform(run, workspace, listener);

        verify(action).doPerform(themisInstance, run, workspace, listener);
    }

    @Test
    public void testPerformUnknownInstance() {
        GlobalConfiguration.all().get(ThemisGlobalConfiguration.class).getInstances().clear();

        action.perform(run, workspace, listener);

        verify(logger, never()).println(anyString());
        verify(listener, only()).error("Unknown Themis instance: " + INSTANCE_NAME);
    }

    @Test(expected = RuntimeException.class)
    public void testPerformUnknownInstanceFailBuild() {
        GlobalConfiguration.all().get(ThemisGlobalConfiguration.class).getInstances().clear();
        action.setFailBuild(true);

        action.perform(run, workspace, listener);
    }

    private static class ThemisActionImpl extends ThemisAction {

        ThemisActionImpl(String instanceName) {
            super(instanceName);
        }

        @Override
        void doPerform(ThemisInstance instance, Run<?, ?> run, FilePath workspace,
                       TaskListener listener) {

        }

    }

}
