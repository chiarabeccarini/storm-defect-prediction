package it.uniroma2.isw2.storm.metrics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Commit precedenti di ogni autore, riconosciuto per nome, con la data e i sottosistemi toccati. */
final class DeveloperExperience {

    private static final double DAYS_PER_YEAR = 365.25;

    private record PastCommit(LocalDate date, Set<String> subsystems) {
    }

    private final Map<String, List<PastCommit>> commitsByAuthor = new HashMap<>();

    int experience(String authorName) {
        return pastCommits(authorName).size();
    }

    // Ogni commit precedente pesa 1/(1 + anni trascorsi): l'esperienza recente conta più di quella lontana.
    double recentExperience(String authorName, LocalDate commitDate) {
        return pastCommits(authorName).stream()
                .mapToDouble(past -> 1 / (1 + Math.max(0, ChronoUnit.DAYS.between(past.date(), commitDate)) / DAYS_PER_YEAR))
                .sum();
    }

    int subsystemExperience(String authorName, Set<String> subsystems) {
        return (int) pastCommits(authorName).stream()
                .filter(past -> !Collections.disjoint(past.subsystems(), subsystems))
                .count();
    }

    void addCommit(String authorName, LocalDate commitDate, Set<String> subsystems) {
        commitsByAuthor.computeIfAbsent(authorName, key -> new ArrayList<>()).add(new PastCommit(commitDate, Set.copyOf(subsystems)));
    }

    private List<PastCommit> pastCommits(String authorName) {
        return commitsByAuthor.getOrDefault(authorName, List.of());
    }
}
