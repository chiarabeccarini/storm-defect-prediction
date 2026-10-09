package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.git.Commit;
import it.uniroma2.isw2.storm.git.FileChange;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.history.ClassIdentityTracker;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Calcola le metriche di classe di ogni coppia classe-release, facendo il checkout di ciascuna release. */
public final class ClassMetricsExtractor {

    private static final Logger LOGGER = Logger.getLogger(ClassMetricsExtractor.class.getName());

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
        List<String> releaseCommits = releases.stream().map(IncludedRelease::commit).toList();
        List<Commit> history = repository.history(releaseCommits);
        logCommitSizes(history);
        ClassIdentityTracker identities = new ClassIdentityTracker(history);
        LOGGER.info(() -> identities.renamedPaths() + " percorsi ricondotti a una classe precedente tramite le rinomine");
        Map<String, List<ClassRevision>> revisionsByIdentity = revisionsByIdentity(history, identities);
        addImportRevisions(repository.importMerges(releaseCommits), identities, revisionsByIdentity);
        Map<Integer, List<String>> classesByRelease = releaseClasses.stream().collect(Collectors.groupingBy(
                ReleaseClass::releaseId, Collectors.mapping(ReleaseClass::path, Collectors.toList())));
        return metricsPerRelease(releases, classesByRelease, identities, revisionsByIdentity);
    }

    private List<ClassMetrics> metricsPerRelease(List<IncludedRelease> releases, Map<Integer, List<String>> classesByRelease,
                                                 ClassIdentityTracker identities, Map<String, List<ClassRevision>> revisionsByIdentity)
            throws IOException {
        String originalBranch = repository.currentBranch();
        List<ClassMetrics> classMetrics = new ArrayList<>();
        try {
            String previousCommit = null;
            for (IncludedRelease release : releases) {
                classMetrics.addAll(releaseMetrics(release, previousCommit, classesByRelease.getOrDefault(release.releaseId(), List.of()),
                        identities, revisionsByIdentity));
                previousCommit = release.commit();
            }
        } finally {
            repository.checkout(originalBranch);
        }
        return classMetrics;
    }

    // Per la prima release la finestra comprende l'intera storia precedente.
    private List<ClassMetrics> releaseMetrics(IncludedRelease release, String previousCommit, List<String> classPaths,
                                              ClassIdentityTracker identities, Map<String, List<ClassRevision>> revisionsByIdentity)
            throws IOException {
        repository.checkout(release.commit());
        Set<String> releaseHistory = repository.commitsIn(release.commit());
        Set<String> releaseWindow = previousCommit == null
                ? releaseHistory
                : repository.commitsIn(previousCommit + ".." + release.commit());
        List<ClassMetrics> metrics = new ArrayList<>();
        for (String classPath : classPaths) {
            List<ClassRevision> revisions = revisionsByIdentity.getOrDefault(identities.identityOf(classPath), List.of());
            metrics.add(ClassMetricsCalculator.compute(release.releaseId(), classPath, linesOf(repository.fileInWorkTree(classPath)),
                    release.releaseDate(), revisions, releaseWindow, releaseHistory));
        }
        return metrics;
    }

    private Map<String, List<ClassRevision>> revisionsByIdentity(List<Commit> history, ClassIdentityTracker identities) {
        Map<String, List<ClassRevision>> revisions = new HashMap<>();
        for (Commit commit : history) {
            for (FileChange change : commit.changes()) {
                revisions.computeIfAbsent(identities.identityOf(change.newPath()), identity -> new ArrayList<>())
                        .add(toRevision(commit, change));
            }
        }
        return revisions;
    }

    // Un merge di importazione diventa la prima revisione dei file che aggiunge solo se questi non ne hanno già una precedente.
    private void addImportRevisions(List<Commit> importMerges, ClassIdentityTracker identities,
                                    Map<String, List<ClassRevision>> revisionsByIdentity) {
        int importedFiles = 0;
        for (Commit importMerge : importMerges) {
            LocalDate importDate = importMerge.authorDate().toLocalDate();
            for (FileChange addedFile : importMerge.changes()) {
                List<ClassRevision> revisions = revisionsByIdentity.computeIfAbsent(identities.identityOf(addedFile.newPath()),
                        identity -> new ArrayList<>());
                if (revisions.stream().allMatch(revision -> revision.date().isAfter(importDate))) {
                    revisions.add(toRevision(importMerge, addedFile));
                    importedFiles++;
                }
            }
        }
        int fileCount = importedFiles;
        LOGGER.info(() -> importMerges.size() + " merge di importazione: " + fileCount + " file senza revisioni precedenti");
    }

    private ClassRevision toRevision(Commit commit, FileChange change) {
        return new ClassRevision(commit.hash(), commit.authorDate().toLocalDate(), commit.authorName(),
                change.addedLines(), change.deletedLines(), commit.changes().size(), fixDetector.isFix(commit.message()));
    }

    // ISO-8859-1 non fallisce mai la decodifica: il numero di righe non dipende dalla codifica del file.
    private static int linesOf(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            return (int) reader.lines().count();
        }
    }

    private static void logCommitSizes(List<Commit> history) {
        List<Integer> sizes = history.stream().map(commit -> commit.changes().size()).sorted().toList();
        if (sizes.isEmpty()) {
            return;
        }
        long bulkCommits = sizes.stream().filter(size -> size > WindowMetrics.BULK_COMMIT_FILES).count();
        LOGGER.info(() -> String.format("File per commit su %d commit: mediana %d, 95o percentile %d, 99o percentile %d, "
                        + "commit con piu' di %d file: %d", sizes.size(), percentile(sizes, 50), percentile(sizes, 95),
                percentile(sizes, 99), WindowMetrics.BULK_COMMIT_FILES, bulkCommits));
    }

    private static int percentile(List<Integer> sortedValues, int percent) {
        return sortedValues.get((int) Math.round(percent / 100.0 * (sortedValues.size() - 1)));
    }
}
