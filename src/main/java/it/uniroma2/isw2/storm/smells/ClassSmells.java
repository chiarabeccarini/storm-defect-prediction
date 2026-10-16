package it.uniroma2.isw2.storm.smells;

public record ClassSmells(int releaseId, String classPath, int smells) {

    public ClassSmells {
        if (classPath == null || classPath.isBlank()) {
            throw new IllegalArgumentException("Classe mancante per la release " + releaseId);
        }
        if (smells < 0) {
            throw new IllegalArgumentException("Numero di smell negativo per " + classPath);
        }
    }
}
