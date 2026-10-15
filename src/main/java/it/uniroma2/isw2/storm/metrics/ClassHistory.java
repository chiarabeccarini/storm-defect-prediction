package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.git.Commit;
import it.uniroma2.isw2.storm.git.FileChange;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.history.ClassIdentityTracker;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Storia delle classi Java delle release incluse: i commit non di merge dal più vecchio al più recente,
 * l'identità di ogni classe attraverso le rinomine e le revisioni di ciascuna identità.
 */
public final class ClassHistory {

    private static final Logger LOGGER = Logger.getLogger(ClassHistory.class.getName());
    private static final int SHORT_HASH_LENGTH = 9;

    private final List<Commit> commits;
    private final ClassIdentityTracker identities;
    private final Map<String, List<ClassRevision>> revisionsByIdentity;

    private ClassHistory(List<Commit> commits, ClassIdentityTracker identities,
                         Map<String, List<ClassRevision>> revisionsByIdentity) {
        this.commits = commits;
        this.identities = identities;
        this.revisionsByIdentity = revisionsByIdentity;
    }

    public static ClassHistory load(GitRepository repository, List<String> releaseCommits, FixCommitDetector fixDetector)
            throws IOException {
        if (releaseCommits.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release di cui leggere la storia");
        }
        List<Commit> commits = repository.history(releaseCommits);
        logCommitSizes(commits);
        logMechanicalCommits(commits);
        ClassIdentityTracker identities = new ClassIdentityTracker(commits);
        LOGGER.info(() -> identities.renamedPaths() + " percorsi ricondotti a una classe precedente tramite le rinomine");
        Map<String, List<ClassRevision>> revisions = revisionsByIdentity(commits, identities, fixDetector);
        addImportRevisions(repository.importMerges(releaseCommits), identities, revisions, fixDetector);
        return new ClassHistory(commits, identities, revisions);
    }

    public List<Commit> commits() {
        return commits;
    }

    public String identityOf(String path) {
        return identities.identityOf(path);
    }

    /** Revisioni della classe, con i percorsi che aveva prima delle rinomine. */
    public List<ClassRevision> revisionsOf(String classPath) {
        return revisionsByIdentity.getOrDefault(identities.identityOf(classPath), List.of());
    }

    private static Map<String, List<ClassRevision>> revisionsByIdentity(List<Commit> commits, ClassIdentityTracker identities,
                                                                        FixCommitDetector fixDetector) {
        Map<String, List<ClassRevision>> revisions = new HashMap<>();
        for (Commit commit : commits) {
            for (FileChange change : commit.changes().stream().filter(FileChange::isJavaFile).toList()) {
                revisions.computeIfAbsent(identities.identityOf(change.newPath()), identity -> new ArrayList<>())
                        .add(toRevision(commit, change, fixDetector));
            }
        }
        return revisions;
    }

    // Un merge di importazione diventa la prima revisione dei file che aggiunge solo se questi non ne hanno già una precedente.
    private static void addImportRevisions(List<Commit> importMerges, ClassIdentityTracker identities,
                                           Map<String, List<ClassRevision>> revisionsByIdentity, FixCommitDetector fixDetector) {
        int importedFiles = 0;
        for (Commit importMerge : importMerges) {
            LocalDate importDate = importMerge.authorDate().toLocalDate();
            for (FileChange addedFile : importMerge.changes().stream().filter(FileChange::isJavaFile).toList()) {
                List<ClassRevision> revisions = revisionsByIdentity.computeIfAbsent(identities.identityOf(addedFile.newPath()),
                        identity -> new ArrayList<>());
                if (revisions.stream().allMatch(revision -> revision.date().isAfter(importDate))) {
                    revisions.add(toRevision(importMerge, addedFile, fixDetector));
                    importedFiles++;
                }
            }
        }
        int fileCount = importedFiles;
        LOGGER.info(() -> importMerges.size() + " merge di importazione: " + fileCount + " file senza revisioni precedenti");
    }

    private static ClassRevision toRevision(Commit commit, FileChange change, FixCommitDetector fixDetector) {
        return new ClassRevision(commit.hash(), commit.authorDate().toLocalDate(), commit.authorName(), change.oldPath(),
                change.created(), change.addedLines(), change.deletedLines(), commit.javaFileCount(), BulkCommits.isBulk(commit),
                BulkCommits.isMechanical(commit), fixDetector.isFix(commit.message()));
    }

    private static void logCommitSizes(List<Commit> commits) {
        List<Integer> sizes = commits.stream().map(commit -> commit.changes().size()).sorted().toList();
        if (sizes.isEmpty()) {
            return;
        }
        long bulkCommits = sizes.stream().filter(size -> size > BulkCommits.FILE_THRESHOLD).count();
        LOGGER.info(() -> String.format("File per commit su %d commit: mediana %d, 95o percentile %d, 99o percentile %d, "
                        + "commit con piu' di %d file: %d", sizes.size(), percentile(sizes, 50), percentile(sizes, 95),
                percentile(sizes, 99), BulkCommits.FILE_THRESHOLD, bulkCommits));
    }

    private static void logMechanicalCommits(List<Commit> commits) {
        List<String> mechanicalCommits = commits.stream()
                .filter(commit -> BulkCommits.isMechanical(commit) && commit.javaFileCount() > 0)
                .map(commit -> commit.hash().substring(0, SHORT_HASH_LENGTH) + " (" + commit.changes().size() + " file, "
                        + commit.javaFileCount() + " .java)")
                .toList();
        LOGGER.info(() -> "Commit meccanici che toccano classi Java, esclusi dalle metriche: " + mechanicalCommits.size()
                + " " + mechanicalCommits);
    }

    private static int percentile(List<Integer> sortedValues, int percent) {
        return sortedValues.get((int) Math.round(percent / 100.0 * (sortedValues.size() - 1)));
    }
}
