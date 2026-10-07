package it.uniroma2.isw2.storm.release;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

public record Release(String jiraId, String name, LocalDate releaseDate) {

    private static final Pattern VERSION_NUMBER_PART = Pattern.compile("\\d+");

    public static final Comparator<Release> BY_VERSION_NUMBER =
            Comparator.comparing(Release::versionNumber, Arrays::compare);

    // A parità di data si ordina per numero di versione, così l'ordine non dipende
    // da come Jira restituisce le versioni rilasciate nello stesso giorno.
    public static final Comparator<Release> CHRONOLOGICAL =
            Comparator.comparing(Release::releaseDate).thenComparing(BY_VERSION_NUMBER);

    public Release {
        Objects.requireNonNull(releaseDate, "releaseDate");
        if (jiraId == null || jiraId.isBlank()) {
            throw new IllegalArgumentException("Identificativo Jira mancante");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome della versione mancante (id " + jiraId + ")");
        }
    }

    public int[] versionNumber() {
        return VERSION_NUMBER_PART.matcher(name).results()
                .mapToInt(match -> Integer.parseInt(match.group()))
                .toArray();
    }
}
