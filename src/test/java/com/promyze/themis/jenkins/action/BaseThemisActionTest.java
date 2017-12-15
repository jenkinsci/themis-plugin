package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.ThemisGlobalConfiguration;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import com.promyze.themis.jenkins.test.MockThemis;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.PrintStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BaseThemisActionTest<T extends ThemisAction> {

    private static final String URL = "http://localhost:";
    static final String INSTANCE_NAME = "instance";
    static final String API_KEY = "apiKey";

    T action;
    MockThemis themis;
    ThemisInstance themisInstance;
    Run<?, ?> run;
    FilePath workspace;
    TaskListener listener;
    PrintStream logger;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setupEnvironment() throws IOException {
        themis = new MockThemis();
        int port = themis.start();
        themisInstance = new ThemisInstance(INSTANCE_NAME, URL + port, API_KEY);
        GlobalConfiguration.all().get(ThemisGlobalConfiguration.class).getInstances().add(themisInstance);

        run = mock(Run.class);
        workspace = mock(FilePath.class);
        listener = mock(TaskListener.class);
        logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);
    }

    @After
    public void stopThemis() {
        themis.stop();
    }

}
