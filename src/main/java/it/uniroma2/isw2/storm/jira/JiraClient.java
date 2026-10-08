package it.uniroma2.isw2.storm.jira;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class JiraClient {

    private static final Pattern PROJECT_KEY = Pattern.compile("[A-Z][A-Z0-9]+");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    // Jira restituisce al massimo 1000 risultati per richiesta: le ricerche procedono a blocchi.
    private static final int PAGE_SIZE = 1000;

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
        return getJson(URI.create(jiraUrl + "/rest/api/2/project/" + projectKey));
    }

    public List<JSONObject> searchIssues(String jql, List<String> fields) throws IOException {
        if (jql == null || jql.isBlank() || fields.isEmpty()) {
            throw new IllegalArgumentException("Query JQL o campi della ricerca mancanti");
        }
        List<JSONObject> issues = new ArrayList<>();
        int total;
        do {
            JSONObject page = getJson(searchUri(jql, fields, issues.size()));
            JSONArray pageIssues = page.getJSONArray("issues");
            total = page.getInt("total");
            if (pageIssues.isEmpty() && issues.size() < total) {
                throw new IOException("Jira ha restituito una pagina vuota prima della fine dei risultati");
            }
            IntStream.range(0, pageIssues.length()).mapToObj(pageIssues::getJSONObject).forEach(issues::add);
        } while (issues.size() < total);
        return issues;
    }

    private URI searchUri(String jql, List<String> fields, int startAt) {
        return URI.create(jiraUrl + "/rest/api/2/search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8)
                + "&fields=" + String.join(",", fields) + "&startAt=" + startAt + "&maxResults=" + PAGE_SIZE);
    }

    private JSONObject getJson(URI uri) throws IOException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build());
        if (response.statusCode() != 200) {
            throw new IOException("Jira ha risposto con stato " + response.statusCode() + " a " + uri);
        }
        try {
            return new JSONObject(response.body());
        } catch (JSONException e) {
            throw new IOException("Risposta di Jira non in formato JSON da " + uri, e);
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
