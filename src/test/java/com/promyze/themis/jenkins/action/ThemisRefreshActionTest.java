package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.test.MockThemis.RefreshHandler;
import org.junit.Before;
import org.junit.Test;

import static com.promyze.themis.jenkins.test.ThemisAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

public class ThemisRefreshActionTest extends BaseThemisActionTest<ThemisRefreshAction> {

    private static final String PROJECT_KEY = "projectKey";
    private static final String PATH = "/api/refreshProject/" + PROJECT_KEY;
    private static final String OK_MESSAGE = "{\"dataDisplayed\": \"Refresh project\"}";

    @Before
    public void setupAction() {
        action = new ThemisRefreshAction(INSTANCE_NAME, PROJECT_KEY);
    }

    @Test
    public void testGetProjectKey() {
        assertThat(action.getProjectKey()).isEqualTo(PROJECT_KEY);
    }

    @Test
    public void testPerformNominal() {
        RefreshHandler okHandler = new RefreshHandler(OK_MESSAGE);
        themis.setRefreshHandler(API_KEY, PATH, okHandler);

        action.perform(run, workspace, listener);

        assertThat(okHandler).isOK();
        verify(listener, never()).error(anyString());
    }

    @Test
    public void testPerformWrongApiKey() {
        RefreshHandler okHandler = new RefreshHandler(OK_MESSAGE);
        themis.setRefreshHandler("otherApiKey", PATH, okHandler);

        action.perform(run, workspace, listener);

        assertThat(okHandler).isKO();
        verify(logger, never()).println(anyString());
        verify(listener, only()).error(anyString());
    }

    @Test
    public void testPerformWrongProjectKey() {
        RefreshHandler okHandler = new RefreshHandler(OK_MESSAGE);
        themis.setRefreshHandler(API_KEY, "/api/refreshProject/otherProjectKey", okHandler);

        action.perform(run, workspace, listener);

        assertThat(okHandler).isKO();
        verify(logger, never()).println(anyString());
        verify(listener, only()).error(anyString());
    }

    @Test
    public void testPerformUnavailableServer() {
        themis.stop();

        action.perform(run, workspace, listener);

        verify(listener).error(anyString());
        verify(logger, only()).println(contains("Connection refused"));
    }

}
