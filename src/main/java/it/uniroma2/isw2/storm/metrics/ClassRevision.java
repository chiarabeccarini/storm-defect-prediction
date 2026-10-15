package it.uniroma2.isw2.storm.metrics;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Modifica di una classe in un commit; il percorso precedente è quello che il file aveva prima del commit
 * (diverso dall'attuale solo in caso di rinomina) e il change set conta i soli file .java del commit.
 */
public record ClassRevision(String commitHash, LocalDate date, String authorName, String previousPath, boolean created,
                            int addedLines, int deletedLines, int changeSetSize, boolean bulkCommit, boolean mechanicalCommit,
                            boolean fix) {

    public ClassRevision {
        Objects.requireNonNull(commitHash, "commitHash");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(authorName, "authorName");
        Objects.requireNonNull(previousPath, "previousPath");
        if (changeSetSize < 1) {
            throw new IllegalArgumentException("Il commit " + commitHash + " deve toccare almeno un file .java");
        }
    }

    public int touchedLines() {
        return addedLines + deletedLines;
    }
}
