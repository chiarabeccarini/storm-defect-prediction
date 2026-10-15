package it.uniroma2.isw2.storm.metrics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Modifiche precedenti di ogni file, seguito attraverso le rinomine: autori, commit e data dell'ultima modifica. */
final class FileChangeHistory {

    private static final double DAYS_PER_WEEK = 7.0;

    private final Map<String, Set<String>> authorsByFile = new HashMap<>();
    private final Map<String, Set<String>> commitsByFile = new HashMap<>();
    private final Map<String, LocalDate> lastChangeByFile = new HashMap<>();

    int developers(Collection<String> files) {
        return union(authorsByFile, files).size();
    }

    int uniqueChanges(Collection<String> files) {
        return union(commitsByFile, files).size();
    }

    // Un file creato dal commit vale 0: è il codice più giovane possibile, quindi il più esposto ai difetti.
    double averageAgeWeeks(Collection<String> files, LocalDate commitDate) {
        return files.stream()
                .mapToDouble(file -> ageWeeks(lastChangeByFile.get(file), commitDate))
                .average()
                .orElse(0);
    }

    void addChange(Collection<String> files, String commitHash, String authorName, LocalDate commitDate) {
        for (String file : files) {
            authorsByFile.computeIfAbsent(file, key -> new HashSet<>()).add(authorName);
            commitsByFile.computeIfAbsent(file, key -> new HashSet<>()).add(commitHash);
            lastChangeByFile.put(file, commitDate);
        }
    }

    private static double ageWeeks(LocalDate lastChange, LocalDate commitDate) {
        if (lastChange == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(lastChange, commitDate)) / DAYS_PER_WEEK;
    }

    private static Set<String> union(Map<String, Set<String>> valuesByFile, Collection<String> files) {
        Set<String> union = new HashSet<>();
        files.forEach(file -> union.addAll(valuesByFile.getOrDefault(file, Set.of())));
        return union;
    }
}
