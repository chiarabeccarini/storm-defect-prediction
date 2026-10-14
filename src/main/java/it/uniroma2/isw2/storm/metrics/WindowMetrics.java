package it.uniroma2.isw2.storm.metrics;

import java.util.List;
import java.util.stream.Collectors;

/** Metriche di una classe calcolate su un insieme di revisioni (la sola release oppure l'intera storia). */
public record WindowMetrics(int revisions, int fixes, int authors, Distribution addedLines, Distribution churn,
                            Distribution changeSet) {

    public record Distribution(int sum, int max, double average) {

        static Distribution of(List<Integer> values) {
            return new Distribution(values.stream().mapToInt(Integer::intValue).sum(),
                    values.stream().mapToInt(Integer::intValue).max().orElse(0),
                    values.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
    }

    // I commit di massa rimasti nel calcolo (quelli che creano classi) restano esclusi dal solo change set.
    public static WindowMetrics of(List<ClassRevision> revisions) {
        List<Integer> changeSets = revisions.stream()
                .filter(revision -> !revision.bulkCommit())
                .map(ClassRevision::changeSetSize)
                .toList();
        int fixes = (int) revisions.stream().filter(ClassRevision::fix).count();
        int authors = revisions.stream().map(ClassRevision::authorName).collect(Collectors.toSet()).size();
        return new WindowMetrics(revisions.size(), fixes, authors,
                Distribution.of(revisions.stream().map(ClassRevision::addedLines).toList()),
                Distribution.of(revisions.stream().map(ClassRevision::touchedLines).toList()),
                Distribution.of(changeSets));
    }
}
