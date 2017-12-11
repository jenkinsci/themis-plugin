package com.promyze.themis.jenkins.action;

import com.promyze.themis.jenkins.HttpClientUtils;
import com.promyze.themis.jenkins.Messages;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration;
import com.promyze.themis.jenkins.ThemisGlobalConfiguration.ThemisInstance;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Action that sends a request the refresh of a Themis project.
 */
public class ThemisRefreshAction extends ThemisAction {

    private static final long serialVersionUID = 1L;

    private static final String REFRESH_URL_FORMAT = "{0}/api/refreshProject/{1}";

    private final String projectKey;

    /**
     * Default constructor.
     *
     * @param instanceName the name of the Themis instance to use, as specified in the Jenkins global configuration
     * @param projectKey   the unique key of the project to refresh
     */
    public ThemisRefreshAction(String instanceName, String projectKey) {
        super(instanceName);
        this.projectKey = projectKey;
    }

    /**
     * @return the unique key of the project to refresh
     */
    public String getProjectKey() {
        return projectKey;
    }

    @Override
    void doPerform(ThemisInstance instance, Run<?, ?> run, FilePath workspace, TaskListener listener) {
        try (CloseableHttpClient httpClient = HttpClientUtils.getClient();
             CloseableHttpResponse response = refreshThemis(httpClient, instance)) {
            String body = EntityUtils.toString(response.getEntity());
            if (isSuccessful(response)) {
                JSONObject result = new JSONObject(body);
                listener.getLogger().println(Messages.projectRefreshed(result.get("dataDisplayed")));
            } else {
                fail(listener, Messages.refreshError(response.getStatusLine().getStatusCode(), body));
            }
        } catch (IOException e) {
            fail(listener, Messages.themisUnknownError(instance.getName()), e);
        }
    }

    private boolean isSuccessful(CloseableHttpResponse response) {
        return response.getStatusLine().getStatusCode() == 200;
    }

    private CloseableHttpResponse refreshThemis(CloseableHttpClient client, ThemisInstance instance)
            throws IOException {
        String url = MessageFormat.format(REFRESH_URL_FORMAT, instance.getUrl(), projectKey);
        HttpGet request = new HttpGet(url);
        request.setHeader(ThemisGlobalConfiguration.THEMIS_API_KEY, instance.getApiKey());
        return client.execute(request);
    }

}
