package it.uniroma2.isw2.storm.labeling;

/** Versioni di un ticket sulla linea principale; la classe toccata dal fix è difettosa da IV a FV esclusa. */
record TicketLifecycle(String key, int openingVersion, int fixedVersion, int injectedVersion) {

    TicketLifecycle {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Chiave del ticket mancante");
        }
        if (openingVersion < 1 || fixedVersion < 1 || injectedVersion < 1) {
            throw new IllegalArgumentException("Versioni non valide per il ticket " + key);
        }
        if (injectedVersion > openingVersion) {
            throw new IllegalArgumentException("Injected Version successiva alla Opening Version per il ticket " + key);
        }
    }
}
