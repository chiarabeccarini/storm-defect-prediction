package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Calcola le metriche di classe di ogni coppia classe-release, facendo il checkout di ciascuna release. */
public final class ClassMetricsExtractor {

    private final GitRepository repository;
    private final FixCommitDetector fixDetector;

    public ClassMetricsExtractor(GitRepository repository, FixCommitDetector fixDetector) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.fixDetector = Objects.requireNonNull(fixDetector, "fixDetector");
    }

    public List<ClassMetrics> extract(List<IncludedRelease> releases, List<ReleaseClass> releaseClasses) throws IOException {
        if (releases.isEmpty() || releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Release o classi mancanti per il calcolo delle metriche");
        }
        ClassHistory history = ClassHistory.load(repository, releases.stream().map(IncludedRelease::commit).toList(), fixDetector);
        Map<Integer, List<String>> classesByRelease = releaseClasses.stream().collect(Collectors.groupingBy(
                ReleaseClass::releaseId, Collectors.mapping(ReleaseClass::path, Collectors.toList())));
        return metricsPerRelease(releases, classesByRelease, history);
    }

    private List<ClassMetrics> metricsPerRelease(List<IncludedRelease> releases, Map<Integer, List<String>> classesByRelease,
                                                 ClassHistory history) throws IOException {
        String originalBranch = repository.currentBranch();
        List<ClassMetrics> classMetrics = new ArrayList<>();
        try {
            String previousCommit = null;
            for (IncludedRelease release : releases) {
                classMetrics.addAll(releaseMetrics(release, previousCommit,
                        classesByRelease.getOrDefault(release.releaseId(), List.of()), history));
                previousCommit = release.commit();
            }
        } finally {
            repository.checkout(originalBranch);
        }
        return classMetrics;
    }

    private List<ClassMetrics> releaseMetrics(IncludedRelease release, String previousCommit, List<String> classPaths,
                                              ClassHistory history) throws IOException {
        repository.checkout(release.commit());
        ReleaseCommits releaseCommits = ReleaseCommits.of(repository, release, previousCommit);
        return classPaths.stream()
                .map(classPath -> ClassMetricsCalculator.compute(release.releaseId(), classPath,
                        linesOf(repository.fileInWorkTree(classPath)), release.releaseDate(), history.revisionsOf(classPath),
                        releaseCommits.window(), releaseCommits.history()))
                .toList();
    }

    // ISO-8859-1 non fallisce mai la decodifica: il numero di righe non dipende dalla codifica del file.
    private static int linesOf(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            return (int) reader.lines().count();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile contare le righe di " + file, e);
        }
    }
}
