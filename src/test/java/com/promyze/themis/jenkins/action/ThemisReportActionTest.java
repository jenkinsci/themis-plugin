package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.ReportFile;
import com.promyze.themis.jenkins.test.MockThemis.ReportHandler;
import hudson.EnvVars;
import hudson.FilePath;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.promyze.themis.jenkins.test.MockThemis.response;
import static com.promyze.themis.jenkins.test.ThemisAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThemisReportActionTest extends BaseThemisActionTest<ThemisReportAction> {

    private static final String SOURCE_KEY = "sourceKey";
    private static final String PATH = "/api/reportFiles/" + SOURCE_KEY;
    private static final String TYPE = "type";
    private static final String FILE_PATH = "target/**/*.xml";
    private static final String COMMIT_ID = "f3dcc900830d696fb6faf2d9ecc8589e3c942559";
    private static final long DATE = 1513330262227L;
    private static final String WORKSPACE = "/jenkins/workspace";

    @Before
    public void setupAction() {
        action = new ThemisReportAction(INSTANCE_NAME, SOURCE_KEY);
    }

    @Test
    public void testGetSourceKey() {
        assertThat(action.getSourceKey()).isEqualTo(SOURCE_KEY);
    }

    @Test
    public void testGetReportsEmpty() {
        assertThat(action.getReports()).isEmpty();
    }

    @Test
    public void testAddReportFile() {
        ReportFile reportFile = new ReportFile(TYPE, FILE_PATH);

        action.addReportFile(reportFile);

        assertThat(action.getReports()).containsOnly(entry(TYPE, Collections.singletonList(FILE_PATH)));
    }

    @Test
    public void testAddReport() {
        Map<String, String> report = new HashMap<>();
        report.put(ThemisReportAction.TYPE_KEY, TYPE);
        report.put(ThemisReportAction.PATH_KEY, FILE_PATH);
        action.addReport(report);

        assertThat(action.getReports()).containsOnly(entry(TYPE, Collections.singletonList(FILE_PATH)));
    }

    @Test
    public void testPerformNominal() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        EnvVars envVars = new EnvVars();
        envVars.put("GIT_COMMIT", COMMIT_ID);
        when(run.getEnvironment(listener)).thenReturn(envVars);
        when(run.getStartTimeInMillis()).thenReturn(DATE);
        when(workspace.getRemote()).thenReturn(WORKSPACE);
        when(workspace.list(FILE_PATH)).thenReturn(new FilePath[1]);
        doAnswer(i -> {
            ((Closeable) i.getArgument(0)).close();
            return null;
        }).when(workspace).zip(any(), anyString());

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(1);
        assertThat(handler).hasRequest(TYPE, new JSONObject()
                .put("commit", COMMIT_ID)
                .put("dataType", TYPE)
                .put("executionDate", DATE)
                .put("dataWorkspace", WORKSPACE));
    }

    @Test
    public void testPerformMultipleTypes() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        action.addReportFile(new ReportFile(TYPE + 2, FILE_PATH));
        EnvVars envVars = new EnvVars();
        envVars.put("SVN_REVISION", COMMIT_ID);
        action.setEnvVars(envVars);
        when(run.getStartTimeInMillis()).thenReturn(DATE);
        when(workspace.getRemote()).thenReturn(WORKSPACE);
        when(workspace.list(FILE_PATH)).thenReturn(new FilePath[1]);
        doAnswer(i -> {
            ((Closeable) i.getArgument(0)).close();
            return null;
        }).when(workspace).zip(any(), anyString());

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(2);
        assertThat(handler).hasRequest(TYPE, new JSONObject()
                .put("commit", COMMIT_ID)
                .put("dataType", TYPE)
                .put("executionDate", DATE)
                .put("dataWorkspace", WORKSPACE));
        assertThat(handler).hasRequest(TYPE + 2, new JSONObject()
                .put("commit", COMMIT_ID)
                .put("dataType", TYPE + 2)
                .put("executionDate", DATE)
                .put("dataWorkspace", WORKSPACE));
    }

    @Test
    public void testPerformNoFiles() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        when(run.getEnvironment(listener)).thenReturn(new EnvVars());
        when(workspace.list(FILE_PATH)).thenReturn(new FilePath[0]);

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(0);
        verify(workspace, never()).zip(any(), anyString());
    }

    @Test
    public void testPerformGetEnvException() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        when(run.getEnvironment(listener)).thenThrow(IOException.class);

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(0);
        verify(workspace, never()).zip(any(), anyString());
        verify(listener, atLeastOnce()).error(anyString());
    }

    @Test
    public void testPerformExceptionInSendReport() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        when(run.getEnvironment(listener)).thenReturn(new EnvVars());
        when(workspace.list(FILE_PATH)).thenThrow(IOException.class);

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(0);
        verify(workspace, never()).zip(any(), anyString());
        verify(listener, atLeastOnce()).error(anyString());
    }

    @Test
    public void testPerformExceptionInZip() throws IOException, InterruptedException {
        ReportHandler handler = new ReportHandler();
        themis.setReporHandler(API_KEY, PATH, handler);
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        EnvVars envVars = new EnvVars();
        envVars.put("GIT_COMMIT", COMMIT_ID);
        when(run.getEnvironment(listener)).thenReturn(envVars);
        when(run.getStartTimeInMillis()).thenReturn(DATE);
        when(workspace.getRemote()).thenReturn(WORKSPACE);
        when(workspace.list(FILE_PATH)).thenReturn(new FilePath[1]);
        doThrow(IOException.class).when(workspace).zip(any(), anyString());

        action.perform(run, workspace, listener);

        assertThat(handler).hasReports(0);
        verify(listener, atLeastOnce()).error(anyString());
    }

    @Test
    public void testPerformServerError() throws IOException, InterruptedException {
        themis.setReporHandler(API_KEY, PATH, e -> response(500, "Error"));
        action.addReportFile(new ReportFile(TYPE, FILE_PATH));
        EnvVars envVars = new EnvVars();
        envVars.put("GIT_COMMIT", COMMIT_ID);
        when(run.getEnvironment(listener)).thenReturn(envVars);
        when(run.getStartTimeInMillis()).thenReturn(DATE);
        when(workspace.getRemote()).thenReturn(WORKSPACE);
        when(workspace.list(FILE_PATH)).thenReturn(new FilePath[1]);
        doAnswer(i -> {
            ((Closeable) i.getArgument(0)).close();
            return null;
        }).when(workspace).zip(any(), anyString());

        action.perform(run, workspace, listener);

        verify(listener, atLeastOnce()).error(anyString());
    }

}
