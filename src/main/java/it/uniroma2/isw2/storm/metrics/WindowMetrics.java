package it.uniroma2.isw2.storm.metrics;

import java.util.List;
import java.util.stream.Collectors;

/** Metriche di una classe calcolate su un insieme di revisioni (la sola release oppure l'intera storia). */
public record WindowMetrics(int revisions, int fixes, int authors, Distribution addedLines, Distribution churn,
                            Distribution changeSet) {

    // I commit che toccano più file di questa soglia sono riorganizzazioni di massa: esclusi dal solo change set.
    static final int BULK_COMMIT_FILES = 50;

    public record Distribution(int sum, int max, double average) {

        static Distribution of(List<Integer> values) {
            return new Distribution(values.stream().mapToInt(Integer::intValue).sum(),
                    values.stream().mapToInt(Integer::intValue).max().orElse(0),
                    values.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
    }

    public static WindowMetrics of(List<ClassRevision> revisions) {
        List<Integer> changeSets = revisions.stream()
                .map(ClassRevision::commitFileCount)
                .filter(fileCount -> fileCount <= BULK_COMMIT_FILES)
                .toList();
        int fixes = (int) revisions.stream().filter(ClassRevision::fix).count();
        int authors = revisions.stream().map(ClassRevision::authorName).collect(Collectors.toSet()).size();
        return new WindowMetrics(revisions.size(), fixes, authors,
                Distribution.of(revisions.stream().map(ClassRevision::addedLines).toList()),
                Distribution.of(revisions.stream().map(ClassRevision::touchedLines).toList()),
                Distribution.of(changeSets));
    }
}
