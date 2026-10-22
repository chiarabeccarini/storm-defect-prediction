package it.uniroma2.isw2.storm.release;

import java.time.LocalDate;
import java.util.Objects;

/** Release della linea principale, inclusa o no nel dataset: l'etichettatura le usa tutte. */
public record MainlineRelease(int releaseId, String name, LocalDate releaseDate, String commit, boolean included) {

    public MainlineRelease {
        if (releaseId < 1) {
            throw new IllegalArgumentException("Release ID non valido: " + releaseId);
        }
        if (name == null || name.isBlank() || commit == null || commit.isBlank()) {
            throw new IllegalArgumentException("Nome o commit mancante per la release " + releaseId);
        }
        Objects.requireNonNull(releaseDate, "releaseDate");
    }

    public IncludedRelease toIncludedRelease() {
        return new IncludedRelease(releaseId, name, releaseDate, commit);
    }
}
