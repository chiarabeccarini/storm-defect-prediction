package it.uniroma2.isw2.storm.release;

import java.time.LocalDate;
import java.util.Objects;

public record Release(String jiraId, String name, LocalDate releaseDate) {

    public Release {
        Objects.requireNonNull(releaseDate, "releaseDate");
        if (jiraId == null || jiraId.isBlank()) {
            throw new IllegalArgumentException("Identificativo Jira mancante");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome della versione mancante (id " + jiraId + ")");
        }
    }
}
