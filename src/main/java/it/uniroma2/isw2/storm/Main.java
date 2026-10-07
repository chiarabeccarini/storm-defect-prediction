package it.uniroma2.isw2.storm;

import it.uniroma2.isw2.storm.jira.JiraClient;
import it.uniroma2.isw2.storm.jira.ReleaseExtractor;
import it.uniroma2.isw2.storm.release.Release;
import it.uniroma2.isw2.storm.release.ReleaseCsvWriter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String PROJECT_KEY = "STORM";
    private static final URI JIRA_URL = URI.create("https://issues.apache.org/jira");
    private static final Path VERSION_INFO_CSV = Path.of("data", PROJECT_KEY + "VersionInfo.csv");

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        List<Release> releases = ReleaseExtractor.datedReleases(new JiraClient(JIRA_URL).fetchProject(PROJECT_KEY));
        ReleaseCsvWriter.write(releases, VERSION_INFO_CSV);
        LOGGER.info(() -> releases.size() + " versioni datate salvate in " + VERSION_INFO_CSV);
    }
}
