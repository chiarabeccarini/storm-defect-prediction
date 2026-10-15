package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.git.Commit;
import it.uniroma2.isw2.storm.git.FileChange;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcola le metriche di ogni commit percorrendo la storia dal più vecchio al più recente: "precedente" indica
 * i commit già incontrati. Sono misurati, e contano come modifiche precedenti ed esperienza, solo i commit non
 * meccanici che modificano almeno un file .java; gli altri file del commit non entrano nel calcolo.
 */
final class CommitMetricsCalculator {

    private final ClassHistory history;
    private final FileChangeHistory fileChanges = new FileChangeHistory();
    private final DeveloperExperience developers = new DeveloperExperience();

    private CommitMetricsCalculator(ClassHistory history) {
        this.history = history;
    }

    static Map<String, CommitMetrics> compute(ClassHistory history) {
        CommitMetricsCalculator calculator = new CommitMetricsCalculator(history);
        Map<String, CommitMetrics> metricsByCommit = new HashMap<>();
        for (Commit commit : history.commits()) {
            if (BulkCommits.isMechanical(commit) || commit.javaFileCount() == 0) {
                continue;
            }
            metricsByCommit.put(commit.hash(), calculator.measureAndRecord(commit));
        }
        return metricsByCommit;
    }

    private CommitMetrics measureAndRecord(Commit commit) {
        List<FileChange> javaChanges = commit.changes().stream().filter(FileChange::isJavaFile).toList();
        Set<String> files = javaChanges.stream().map(change -> history.identityOf(change.newPath())).collect(Collectors.toSet());
        Set<String> subsystems = javaChanges.stream().map(change -> Subsystems.of(change.newPath())).collect(Collectors.toSet());
        LocalDate date = commit.authorDate().toLocalDate();
        String author = commit.authorName();
        CommitMetrics metrics = new CommitMetrics(subsystems.size(), directories(javaChanges), javaChanges.size(),
                entropy(javaChanges), javaChanges.stream().mapToInt(FileChange::addedLines).sum(),
                javaChanges.stream().mapToInt(FileChange::deletedLines).sum(), fileChanges.developers(files),
                fileChanges.averageAgeWeeks(files, date), fileChanges.uniqueChanges(files), developers.experience(author),
                developers.recentExperience(author, date), developers.subsystemExperience(author, subsystems));
        fileChanges.addChange(files, commit.hash(), author, date);
        developers.addCommit(author, date, subsystems);
        return metrics;
    }

    private static int directories(List<FileChange> javaChanges) {
        return (int) javaChanges.stream()
                .map(change -> change.newPath().substring(0, Math.max(0, change.newPath().lastIndexOf('/'))))
                .distinct()
                .count();
    }

    // Entropia di Shannon (in bit) della distribuzione delle righe modificate tra i file: 0 se cambia un solo file.
    private static double entropy(List<FileChange> javaChanges) {
        int totalChurn = javaChanges.stream().mapToInt(change -> change.addedLines() + change.deletedLines()).sum();
        if (totalChurn == 0) {
            return 0;
        }
        return javaChanges.stream()
                .mapToDouble(change -> (double) (change.addedLines() + change.deletedLines()) / totalChurn)
                .filter(share -> share > 0)
                .map(share -> -share * Math.log(share) / Math.log(2))
                .sum();
    }
}
