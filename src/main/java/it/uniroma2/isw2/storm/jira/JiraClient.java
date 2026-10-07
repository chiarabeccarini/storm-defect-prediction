package it.uniroma2.isw2.storm.jira;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public final class JiraClient {

    private static final Pattern PROJECT_KEY = Pattern.compile("[A-Z][A-Z0-9]+");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final URI jiraUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public JiraClient(URI jiraUrl) {
        if (!"https".equals(jiraUrl.getScheme())) {
            throw new IllegalArgumentException("L'URL di Jira deve usare https: " + jiraUrl);
        }
        this.jiraUrl = jiraUrl;
    }

    public JSONObject fetchProject(String projectKey) throws IOException {
        if (!PROJECT_KEY.matcher(projectKey).matches()) {
            throw new IllegalArgumentException("Chiave di progetto Jira non valida: " + projectKey);
        }
        URI projectUri = URI.create(jiraUrl + "/rest/api/2/project/" + projectKey);
        HttpResponse<String> response = send(HttpRequest.newBuilder(projectUri).timeout(TIMEOUT).GET().build());
        if (response.statusCode() != 200) {
            throw new IOException("Jira ha risposto con stato " + response.statusCode() + " a " + projectUri);
        }
        try {
            return new JSONObject(response.body());
        } catch (JSONException e) {
            throw new IOException("Risposta di Jira non in formato JSON da " + projectUri, e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Richiesta a Jira interrotta: " + request.uri(), e);
        }
    }
}
