package it.uniroma2.isw2.storm.release;

import java.time.LocalDate;
import java.util.Objects;

public record IncludedRelease(int releaseId, String name, LocalDate releaseDate, String commit) {

    public IncludedRelease {
        if (releaseId < 1) {
            throw new IllegalArgumentException("Release ID non valido: " + releaseId);
        }
        if (name == null || name.isBlank() || commit == null || commit.isBlank()) {
            throw new IllegalArgumentException("Nome o commit mancante per la release " + releaseId);
        }
        Objects.requireNonNull(releaseDate, "releaseDate");
    }
}
