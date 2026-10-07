package it.uniroma2.isw2.storm;

import it.uniroma2.isw2.storm.classes.ProductionClassFilter;
import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.classes.ReleaseClassCsvWriter;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.jira.JiraClient;
import it.uniroma2.isw2.storm.jira.ReleaseExtractor;
import it.uniroma2.isw2.storm.release.IncludedRelease;
import it.uniroma2.isw2.storm.release.IncludedReleaseCsvReader;
import it.uniroma2.isw2.storm.release.MainlineSelector;
import it.uniroma2.isw2.storm.release.Release;
import it.uniroma2.isw2.storm.release.ReleaseCsvReader;
import it.uniroma2.isw2.storm.release.ReleaseCsvWriter;
import it.uniroma2.isw2.storm.release.SelectedRelease;
import it.uniroma2.isw2.storm.release.SelectedReleaseCsvWriter;
import it.uniroma2.isw2.storm.release.TagResolver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String PROJECT_KEY = "STORM";
    private static final URI JIRA_URL = URI.create("https://issues.apache.org/jira");
    private static final Path STORM_REPO = Path.of("C:/isw2/storm");
    // Si ignora il 66% più recente delle release della linea principale, arrotondando per difetto.
    private static final double KEPT_FRACTION = 0.34;

    private static final Path VERSION_INFO_CSV = Path.of("data", PROJECT_KEY + "VersionInfo.csv");
    private static final Path RELEASES_CSV = Path.of("data", PROJECT_KEY + "Releases.csv");
    private static final Path CLASSES_CSV = Path.of("data", PROJECT_KEY + "Classes.csv");

    private static final String USAGE = "Uso: mvn -q compile exec:java \"-Dexec.args=<releases|selection|classes>\"";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(USAGE);
        }
        switch (args[0]) {
            case "releases" -> extractReleases();
            case "selection" -> selectReleases();
            case "classes" -> extractClasses();
            default -> throw new IllegalArgumentException("Passo sconosciuto: " + args[0] + ". " + USAGE);
        }
    }

    private static void extractReleases() throws IOException {
        List<Release> releases = ReleaseExtractor.datedReleases(new JiraClient(JIRA_URL).fetchProject(PROJECT_KEY));
        ReleaseCsvWriter.write(releases, VERSION_INFO_CSV);
        LOGGER.info(() -> releases.size() + " versioni datate salvate in " + VERSION_INFO_CSV);
    }

    private static void selectReleases() throws IOException {
        List<Release> mainline = MainlineSelector.mainline(ReleaseCsvReader.read(VERSION_INFO_CSV));
        int includedCount = (int) Math.floor(mainline.size() * KEPT_FRACTION);
        TagResolver tagResolver = new TagResolver(new GitRepository(STORM_REPO));

        List<SelectedRelease> selectedReleases = new ArrayList<>();
        for (int position = 0; position < mainline.size(); position++) {
            Release release = mainline.get(position);
            TagResolver.ResolvedTag tag = tagResolver.resolve(release);
            selectedReleases.add(new SelectedRelease(position + 1, release, tag.name(), tag.commit(), position < includedCount));
        }
        SelectedReleaseCsvWriter.write(selectedReleases, RELEASES_CSV);
        LOGGER.info(() -> mainline.size() + " release nella linea principale, " + includedCount
                + " incluse nel dataset, salvate in " + RELEASES_CSV);
    }

    private static void extractClasses() throws IOException {
        GitRepository storm = new GitRepository(STORM_REPO);
        String originalBranch = storm.currentBranch();
        List<ReleaseClass> releaseClasses = new ArrayList<>();
        try {
            for (IncludedRelease release : IncludedReleaseCsvReader.read(RELEASES_CSV)) {
                storm.checkout(release.commit());
                storm.trackedFiles().stream()
                        .filter(ProductionClassFilter::isProductionClass)
                        .map(path -> new ReleaseClass(release.releaseId(), path))
                        .forEach(releaseClasses::add);
            }
        } finally {
            storm.checkout(originalBranch);
        }
        ReleaseClassCsvWriter.write(releaseClasses, CLASSES_CSV);
        LOGGER.info(() -> releaseClasses.size() + " coppie classe-release salvate in " + CLASSES_CSV);
    }
}
