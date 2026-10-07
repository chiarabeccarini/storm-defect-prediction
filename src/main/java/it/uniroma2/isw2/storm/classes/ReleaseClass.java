package it.uniroma2.isw2.storm.classes;

public record ReleaseClass(int releaseId, String path) {

    public ReleaseClass {
        if (releaseId < 1) {
            throw new IllegalArgumentException("Release ID non valido: " + releaseId);
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Percorso della classe mancante (release " + releaseId + ")");
        }
    }
}
