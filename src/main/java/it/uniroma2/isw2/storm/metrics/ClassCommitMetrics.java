package it.uniroma2.isw2.storm.metrics;

import java.util.Objects;

/** Metriche dei commit di una classe in una release, sommate sui commit della finestra della release. */
public record ClassCommitMetrics(int releaseId, String classPath, CommitMetrics commitSums, int linesBeforeChanges) {

    public ClassCommitMetrics {
        if (classPath == null || classPath.isBlank()) {
            throw new IllegalArgumentException("Classe mancante per la release " + releaseId);
        }
        Objects.requireNonNull(commitSums, "commitSums");
    }
}
