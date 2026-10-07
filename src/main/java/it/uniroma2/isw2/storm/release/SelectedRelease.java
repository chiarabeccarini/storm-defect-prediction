package it.uniroma2.isw2.storm.release;

import java.util.Objects;

public record SelectedRelease(int releaseId, Release release, String tag, String commit, boolean included) {

    public SelectedRelease {
        Objects.requireNonNull(release, "release");
        if (releaseId < 1) {
            throw new IllegalArgumentException("Release ID non valido: " + releaseId);
        }
        if (tag == null || tag.isBlank() || commit == null || commit.isBlank()) {
            throw new IllegalArgumentException("Tag o commit mancante per la release " + release.name());
        }
    }
}
