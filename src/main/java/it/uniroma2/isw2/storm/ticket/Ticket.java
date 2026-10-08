package it.uniroma2.isw2.storm.ticket;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record Ticket(String key, OffsetDateTime created, OffsetDateTime resolved, List<String> affectedVersions) {

    public Ticket {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Chiave del ticket mancante");
        }
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(resolved, "resolved");
        affectedVersions = List.copyOf(affectedVersions);
    }
}
