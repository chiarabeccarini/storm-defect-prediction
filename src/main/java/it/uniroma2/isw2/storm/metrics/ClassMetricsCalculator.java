package it.uniroma2.isw2.storm.metrics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ClassMetricsCalculator {

    private ClassMetricsCalculator() {
    }

    /**
     * Metriche di una classe in una release, a partire da tutte le revisioni della classe (con
     * i percorsi precedenti alle rinomine) e dai commit della finestra e dell'intera storia della release.
     */
    public static ClassMetrics compute(int releaseId, String classPath, int loc, LocalDate releaseDate,
                                       List<ClassRevision> classRevisions, Set<String> releaseWindow, Set<String> releaseHistory) {
        List<ClassRevision> windowRevisions = within(classRevisions, releaseWindow);
        List<ClassRevision> historyRevisions = within(classRevisions, releaseHistory);
        WindowMetrics total = WindowMetrics.of(historyRevisions);
        long ageWeeks = ageInWeeks(historyRevisions, releaseDate);
        return new ClassMetrics(releaseId, classPath, loc, WindowMetrics.of(windowRevisions), total,
                ageWeeks, ageWeeks * total.churn().sum());
    }

    private static List<ClassRevision> within(List<ClassRevision> classRevisions, Set<String> commits) {
        return classRevisions.stream().filter(revision -> commits.contains(revision.commitHash())).toList();
    }

    // Settimane dal primo commit della classe alla data della release; una data d'autore successiva alla release vale 0.
    private static long ageInWeeks(List<ClassRevision> historyRevisions, LocalDate releaseDate) {
        return historyRevisions.stream()
                .map(ClassRevision::date)
                .min(Comparator.naturalOrder())
                .map(firstDate -> Math.max(0, ChronoUnit.DAYS.between(firstDate, releaseDate) / 7))
                .orElse(0L);
    }
}
