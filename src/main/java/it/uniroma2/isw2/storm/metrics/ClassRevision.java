package it.uniroma2.isw2.storm.metrics;

import java.time.LocalDate;
import java.util.Objects;

public record ClassRevision(String commitHash, LocalDate date, String authorName, int addedLines, int deletedLines,
                            int commitFileCount, boolean fix) {

    public ClassRevision {
        Objects.requireNonNull(commitHash, "commitHash");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(authorName, "authorName");
        if (commitFileCount < 1) {
            throw new IllegalArgumentException("Il commit " + commitHash + " deve toccare almeno un file");
        }
    }

    public int touchedLines() {
        return addedLines + deletedLines;
    }
}
