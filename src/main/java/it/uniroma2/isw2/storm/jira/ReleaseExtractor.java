package it.uniroma2.isw2.storm.jira;

import it.uniroma2.isw2.storm.release.Release;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;

public final class ReleaseExtractor {

    private ReleaseExtractor() {
    }

    /**
     * Restituisce le versioni dotate di data di rilascio, in ordine cronologico.
     * Le versioni uscite nello stesso giorno restano distinte: come trattarle si
     * decide nella selezione delle release.
     */
    public static List<Release> datedReleases(JSONObject jiraProject) {
        JSONArray versions = jiraProject.optJSONArray("versions");
        if (versions == null) {
            throw new IllegalArgumentException("La risposta di Jira non contiene l'elenco delle versioni");
        }
        return IntStream.range(0, versions.length())
                .mapToObj(versions::getJSONObject)
                .filter(version -> version.has("releaseDate"))
                .map(ReleaseExtractor::toRelease)
                .sorted(Release.CHRONOLOGICAL)
                .toList();
    }

    private static Release toRelease(JSONObject version) {
        String name = version.getString("name");
        try {
            return new Release(version.getString("id"), name, LocalDate.parse(version.getString("releaseDate")));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data di rilascio non valida per la versione " + name, e);
        }
    }
}
