package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Somma, per ogni coppia classe-release, le metriche dei commit della finestra della release che modificano la
 * classe. LT somma le righe che la classe aveva subito prima di ciascuno di quei commit, lette dal repository.
 */
public final class CommitMetricsExtractor {

    private static final Logger LOGGER = Logger.getLogger(CommitMetricsExtractor.class.getName());

    private final GitRepository repository;
    private final FixCommitDetector fixDetector;

    public CommitMetricsExtractor(GitRepository repository, FixCommitDetector fixDetector) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.fixDetector = Objects.requireNonNull(fixDetector, "fixDetector");
    }

    public List<ClassCommitMetrics> extract(List<IncludedRelease> releases, List<ReleaseClass> releaseClasses) throws IOException {
        if (releases.isEmpty() || releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Release o classi mancanti per le metriche dei commit");
        }
        ClassHistory history = ClassHistory.load(repository, releases.stream().map(IncludedRelease::commit).toList(), fixDetector);
        Map<String, CommitMetrics> metricsByCommit = CommitMetricsCalculator.compute(history);
        LOGGER.info(() -> metricsByCommit.size() + " commit misurati (non meccanici, con almeno un file .java)");
        Map<Integer, List<String>> classesByRelease = releaseClasses.stream().collect(Collectors.groupingBy(
                ReleaseClass::releaseId, Collectors.mapping(ReleaseClass::path, Collectors.toList())));

        List<ClassCommitMetrics> rows = new ArrayList<>();
        String previousCommit = null;
        for (IncludedRelease release : releases) {
            Set<String> window = ReleaseCommits.of(repository, release, previousCommit).window();
            for (String classPath : classesByRelease.getOrDefault(release.releaseId(), List.of())) {
                rows.add(sumOverWindow(release.releaseId(), classPath, measuredRevisions(history.revisionsOf(classPath), window,
                        metricsByCommit), metricsByCommit));
            }
            LOGGER.info(() -> "Release " + release.name() + ": metriche dei commit calcolate");
            previousCommit = release.commit();
        }
        return rows;
    }

    // Una revisione per commit: se un commit modifica la classe con più percorsi, conta una volta sola.
    private static Collection<ClassRevision> measuredRevisions(List<ClassRevision> revisions, Set<String> window,
                                                               Map<String, CommitMetrics> metricsByCommit) {
        return revisions.stream()
                .filter(revision -> window.contains(revision.commitHash()))
                .filter(revision -> metricsByCommit.containsKey(revision.commitHash()))
                .collect(Collectors.toMap(ClassRevision::commitHash, revision -> revision, (first, second) -> first,
                        LinkedHashMap::new))
                .values();
    }

    private ClassCommitMetrics sumOverWindow(int releaseId, String classPath, Collection<ClassRevision> revisions,
                                             Map<String, CommitMetrics> metricsByCommit) throws IOException {
        CommitMetrics sums = revisions.stream()
                .map(revision -> metricsByCommit.get(revision.commitHash()))
                .reduce(CommitMetrics.ZERO, CommitMetrics::plus);
        int linesBeforeChanges = 0;
        for (ClassRevision revision : revisions) {
            linesBeforeChanges += linesBeforeChange(revision);
        }
        return new ClassCommitMetrics(releaseId, classPath, sums, linesBeforeChanges);
    }

    private int linesBeforeChange(ClassRevision revision) throws IOException {
        if (revision.created()) {
            return 0;
        }
        return repository.linesBefore(revision.commitHash(), revision.previousPath());
    }
}
