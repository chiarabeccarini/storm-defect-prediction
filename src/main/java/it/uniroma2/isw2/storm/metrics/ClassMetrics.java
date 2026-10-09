package it.uniroma2.isw2.storm.metrics;

import java.util.Objects;

public record ClassMetrics(int releaseId, String classPath, int loc, WindowMetrics release, WindowMetrics total,
                           long ageWeeks, long weightedAge) {

    public ClassMetrics {
        if (releaseId < 1 || classPath == null || classPath.isBlank()) {
            throw new IllegalArgumentException("Release ID o classe non validi: " + releaseId + ", " + classPath);
        }
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(total, "total");
        if (loc < 0 || ageWeeks < 0) {
            throw new IllegalArgumentException("LOC o Age negativi per " + classPath);
        }
    }
}
