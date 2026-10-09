package it.uniroma2.isw2.storm.git;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Interpreta l'output di {@code git log --numstat} prodotto con {@link #FORMAT}: i separatori
 * ASCII 0x1E e 0x1F non compaiono nei messaggi, quindi delimitano in modo sicuro commit e campi.
 */
public final class CommitLogParser {

    static final String FORMAT = "%x1e%H%x1f%an%x1f%aI%x1f%B%x1f";

    private static final String COMMIT_SEPARATOR = "\u001e";
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final int FIELDS = 5;
    private static final String RENAME_ARROW = " => ";
    private CommitLogParser() {
    }

    public static List<Commit> parse(String gitLog) {
        return Arrays.stream(gitLog.split(COMMIT_SEPARATOR))
                .filter(commitRecord -> !commitRecord.isBlank())
                .map(CommitLogParser::toCommit)
                .toList();
    }

    private static Commit toCommit(String commitRecord) {
        String[] fields = commitRecord.split(FIELD_SEPARATOR, FIELDS);
        if (fields.length != FIELDS) {
            throw new IllegalArgumentException("Record di git log non valido: " + commitRecord.lines().findFirst().orElse(""));
        }
        List<FileChange> changes = fields[4].lines()
                .filter(line -> !line.isBlank())
                .map(CommitLogParser::toFileChange)
                .toList();
        return new Commit(fields[0].trim(), fields[1], authorDate(fields[2], fields[0]), fields[3].strip(), changes);
    }

    private static OffsetDateTime authorDate(String isoDate, String hash) {
        try {
            return OffsetDateTime.parse(isoDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data non valida nel commit " + hash.trim(), e);
        }
    }

    private static FileChange toFileChange(String numstatLine) {
        String[] columns = numstatLine.split("\t", 3);
        if (columns.length != 3) {
            throw new IllegalArgumentException("Riga numstat non valida: " + numstatLine);
        }
        String[] paths = renamePaths(columns[2]);
        return new FileChange(paths[0], paths[1], lineCount(columns[0]), lineCount(columns[1]));
    }

    // Per i file binari git riporta "-" al posto del numero di righe.
    private static int lineCount(String column) {
        return column.equals("-") ? 0 : Integer.parseInt(column);
    }

    // Git scrive una rinomina come "vecchio => nuovo" oppure, se i percorsi hanno parti in comune,
    // come "prefisso/{vecchio => nuovo}/suffisso".
    private static String[] renamePaths(String pathColumn) {
        int arrow = pathColumn.indexOf(RENAME_ARROW);
        if (arrow < 0) {
            return new String[]{pathColumn, pathColumn};
        }
        int open = pathColumn.lastIndexOf('{', arrow);
        int close = pathColumn.indexOf('}', arrow);
        if (open < 0 || close < 0) {
            return new String[]{pathColumn.substring(0, arrow), pathColumn.substring(arrow + RENAME_ARROW.length())};
        }
        String prefix = pathColumn.substring(0, open);
        String suffix = pathColumn.substring(close + 1);
        String oldPath = prefix + pathColumn.substring(open + 1, arrow) + suffix;
        String newPath = prefix + pathColumn.substring(arrow + RENAME_ARROW.length(), close) + suffix;
        return new String[]{oldPath.replace("//", "/"), newPath.replace("//", "/")};
    }
}
