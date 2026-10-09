package it.uniroma2.isw2.storm.git;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record Commit(String hash, String authorName, OffsetDateTime authorDate, String message, List<FileChange> changes) {

    public Commit {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Hash del commit mancante");
        }
        Objects.requireNonNull(authorName, "authorName");
        Objects.requireNonNull(authorDate, "authorDate");
        Objects.requireNonNull(message, "message");
        changes = List.copyOf(changes);
    }
}
