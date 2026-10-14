package it.uniroma2.isw2.storm.git;

public record FileChange(String oldPath, String newPath, int addedLines, int deletedLines, boolean created) {

    private static final String JAVA_EXTENSION = ".java";

    public FileChange {
        if (newPath == null || newPath.isBlank()) {
            throw new IllegalArgumentException("Percorso del file mancante");
        }
        if (addedLines < 0 || deletedLines < 0) {
            throw new IllegalArgumentException("Numero di righe negativo per " + newPath);
        }
    }

    public boolean isRename() {
        return !oldPath.equals(newPath);
    }

    public boolean isJavaFile() {
        return newPath.endsWith(JAVA_EXTENSION);
    }
}
