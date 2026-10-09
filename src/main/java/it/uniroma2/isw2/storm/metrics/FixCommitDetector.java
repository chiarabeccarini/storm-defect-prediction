package it.uniroma2.isw2.storm.metrics;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Un commit è un fix se il suo messaggio riporta, in qualunque punto, l'identificativo esatto
 * di un ticket di tipo Bug: i confini di parola evitano che STORM-12 corrisponda a STORM-123.
 */
public final class FixCommitDetector {

    private final Pattern ticketKey;
    private final Set<String> bugTicketKeys;

    public FixCommitDetector(String projectKey, Set<String> bugTicketKeys) {
        if (projectKey == null || projectKey.isBlank() || bugTicketKeys.isEmpty()) {
            throw new IllegalArgumentException("Chiave di progetto o ticket di bug mancanti");
        }
        this.ticketKey = Pattern.compile("\\b" + Pattern.quote(projectKey) + "-\\d+\\b");
        this.bugTicketKeys = Set.copyOf(bugTicketKeys);
    }

    public boolean isFix(String commitMessage) {
        return ticketKey.matcher(commitMessage).results()
                .anyMatch(match -> bugTicketKeys.contains(match.group()));
    }
}
