package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.HttpClientUtils;
import com.promyze.themis.jenkins.Messages;
import com.promyze.themis.jenkins.ReportFile;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Action that sends report files (e.g., test coverage reports) to a Themis instance.
 */
public class ThemisReportAction extends ThemisAction {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_KEY = "type";
    public static final String PATH_KEY = "path";

    private static final String REPORT_URL_FORMAT = "{0}/api/reportFiles/{1}";
    private static final String COMMIT_ATTRIBUTE = "commit";
    private static final String BRANCH_ATTRIBUTE = "branch";
    private static final String EXECUTION_DATE_ATTRIBUTE = "executionDate";
    private static final String DATA_WORKSPACE_ATTRIBUTE = "dataWorkspace";
    private static final String DATA_TYPE_ATTRIBUTE = "dataType";

    private final String sourceKey;
    private final Map<String, List<String>> reports = new HashMap<>();
    private EnvVars envVars;

    /**
     * Default constructor
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     * @param sourceKey    the unique key of the source for which to send reports
     */
    public ThemisReportAction(String instanceName, String sourceKey) {
        super(instanceName);
        this.sourceKey = sourceKey;
    }

    /**
     * @return the unique key of the source for which to send reports
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * @return a map that associates report types to a list of file paths
     */
    public Map<String, List<String>> getReports() {
        return reports;
    }

    /**
     * Adds a new report file.
     *
     * @param reportFile the report file to add
     */
    public void addReportFile(ReportFile reportFile) {
        reports.computeIfAbsent(reportFile.getType(), k -> new ArrayList<>()).add(reportFile.getPath());
    }

    /**
     * Adds a new report.
     *
     * @param report a map that contains the report type with {@link #TYPE_KEY} as key and the report path with
     *               {@link #PATH_KEY} as key
     */
    public void addReport(Map<String, String> report) {
        reports.computeIfAbsent(report.get(TYPE_KEY), k -> new ArrayList<>()).add(report.get(PATH_KEY));
    }

    /**
     * Sets the environment variables of the current build.
     *
     * @param envVars the environment variables
     */
    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }

    @Override
    void doPerform(ThemisInstance instance, Run<?, ?> run, FilePath workspace,
                   TaskListener listener) {
        try {
            JSONObject metadata = getMetadata(run, listener, workspace);
            reports.entrySet().parallelStream()
                    .map(e -> sendReport(instance, workspace, copyMetadata(metadata, e.getKey()), e.getValue()))
                    .forEach(r -> handleResult(listener, r));
        } catch (IOException | InterruptedException e) {
            fail(listener, Messages.themisUnknownError(instance.getName()), e);
        }
    }

    private void handleResult(TaskListener listener, Result result) {
        switch (result.status) {
            case SUCCESS:
                listener.getLogger().println(Messages.reportSent(result.type));
                break;
            case ABORTED:
                listener.getLogger().println(Messages.noReportFiles(result.type));
                break;
            case FAILED:
                handleError(listener, result);
                break;
        }
    }

    private void handleError(TaskListener listener, Result result) {
        if (result.exception != null) {
            fail(listener, Messages.reportError(result.type), result.exception);
        } else {
            fail(listener, Messages.reportHttpError(result.statusCode, result.body));
        }
    }

    private JSONObject getMetadata(Run<?, ?> run, TaskListener listener, FilePath workspace)
            throws IOException, InterruptedException {
        JSONObject metadata = new JSONObject();
        addScmInfo(metadata, run, listener);
        return metadata
                .put(EXECUTION_DATE_ATTRIBUTE, run.getStartTimeInMillis())
                .put(DATA_WORKSPACE_ATTRIBUTE, workspace.getRemote());
    }

    private void addScmInfo(JSONObject metadata, Run<?, ?> run, TaskListener listener)
            throws IOException, InterruptedException {
        retrieveEnvVars(run, listener);
        String gitCommit = envVars.get("GIT_COMMIT");
        if (gitCommit != null) {
            metadata.put(COMMIT_ATTRIBUTE, gitCommit);
            metadata.put(BRANCH_ATTRIBUTE, envVars.get("GIT_BRANCH"));
        }
        String svnRevision = envVars.get("SVN_REVISION");
        if (svnRevision != null) {
            metadata.put(COMMIT_ATTRIBUTE, svnRevision);
        }
    }

    private void retrieveEnvVars(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        if (envVars == null) {
            envVars = run.getEnvironment(listener);
        }
    }

    private JSONObject copyMetadata(JSONObject metadata, String type) {
        return new JSONObject(metadata, JSONObject.getNames(metadata)).put(DATA_TYPE_ATTRIBUTE, type);
    }

    private String getType(JSONObject metadata) {
        return metadata.getString(DATA_TYPE_ATTRIBUTE);
    }

    private Result sendReport(ThemisInstance instance, FilePath workspace, JSONObject metadata, List<String> paths) {
        try {
            if (!hasFiles(workspace, paths)) {
                return new Result(getType(metadata));
            }
            return archiveAndSend(instance, workspace, metadata, paths);
        } catch (IOException | InterruptedException | ExecutionException e) {
            return new Result(getType(metadata), e);
        }
    }

    private boolean hasFiles(FilePath workspace, List<String> paths) throws IOException, InterruptedException {
        return workspace.list(String.join(",", paths)).length > 0;
    }

    private Result archiveAndSend(ThemisInstance instance, FilePath workspace, JSONObject metadata, List<String> paths)
            throws IOException, ExecutionException, InterruptedException {
        try (PipedOutputStream outputStream = new PipedOutputStream();
             PipedInputStream inputStream = new PipedInputStream()) {
            outputStream.connect(inputStream);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Future<Exception> archiveTask = submitArchiveTask(executor, outputStream, workspace, paths);
            Future<Result> sendArchiveTask = submitSendArchiveTask(executor, inputStream, instance, metadata);

            return checkResult(getType(metadata), archiveTask.get(), sendArchiveTask.get());
        }
    }

    private Result checkResult(String type, Exception exception, Result result) {
        return exception == null ? result : new Result(type, exception);
    }

    private Future<Exception> submitArchiveTask(ExecutorService executor, PipedOutputStream outputStream,
                                                FilePath workspace, List<String> paths) {
        return executor.submit(() -> {
            try {
                workspace.zip(outputStream, String.join(",", paths));
                return null;
            } catch (IOException | InterruptedException e) {
                return e;
            }
        });
    }

    private Future<Result> submitSendArchiveTask(ExecutorService executor, PipedInputStream inputStream,
                                                 ThemisInstance instance, JSONObject metadata) {
        return executor.submit(() -> {
            try (CloseableHttpClient client = HttpClientUtils.getClient();
                 CloseableHttpResponse response = sendArchive(client, instance, metadata, inputStream)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity());
                return new Result(getType(metadata), statusCode, body);
            }
        });
    }

    private CloseableHttpResponse sendArchive(CloseableHttpClient client, ThemisInstance instance, JSONObject metadata,
                                              InputStream inputStream)
            throws IOException {
        String url = MessageFormat.format(REPORT_URL_FORMAT, instance.getUrl(), sourceKey);

        HttpPost request = new HttpPost(url);
        request.setHeader(THEMIS_API_KEY, instance.getApiKey());
        request.setEntity(getArchiveEntity(metadata, inputStream));

        return client.execute(request);
    }

    private HttpEntity getArchiveEntity(JSONObject metadata, InputStream inputStream) {
        return MultipartEntityBuilder
                .create()
                .addBinaryBody("archive", inputStream, ContentType.create("application/zip"), "archive.zip")
                .addTextBody("metadata", metadata.toString(), ContentType.APPLICATION_JSON)
                .build();
    }

    private static final class Result {

        private final String type;
        private final Status status;
        private final int statusCode;
        private final String body;
        private final Exception exception;

        private Result(String type, Status status, int statusCode, String body, Exception exception) {
            this.type = type;
            this.status = status;
            this.statusCode = statusCode;
            this.body = body;
            this.exception = exception;
        }

        private Result(String type) {
            this(type, Status.ABORTED, -1, null, null);
        }

        private Result(String type, int statusCode, String body) {
            this(type, statusCode == 200 ? Status.SUCCESS : Status.FAILED, statusCode, body, null);
        }

        private Result(String type, Exception exception) {
            this(type, Status.FAILED, -1, null, exception);
        }

    }

    private enum Status {
        SUCCESS, ABORTED, FAILED;
    }

}
