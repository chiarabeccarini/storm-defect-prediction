package it.uniroma2.isw2.storm.release;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public final class MainlineSelector {

    private MainlineSelector() {
    }

    /**
     * Ricostruisce la linea principale di sviluppo: per ogni giorno di rilascio si considera
     * la versione più alta e la si tiene solo se supera quelle di tutti i giorni precedenti.
     * Le release di manutenzione dei rami precedenti, pubblicate dopo versioni più recenti,
     * restano così fuori dalla sequenza.
     */
    public static List<Release> mainline(List<Release> releases) {
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release da cui ricostruire la linea principale");
        }
        NavigableMap<LocalDate, Release> highestPerDay = releases.stream().collect(Collectors.toMap(
                Release::releaseDate, release -> release, BinaryOperator.maxBy(Release.BY_VERSION_NUMBER), TreeMap::new));
        return highestPerDay.values().stream()
                .filter(candidate -> exceedsAll(candidate, highestPerDay.headMap(candidate.releaseDate()).values()))
                .toList();
    }

    private static boolean exceedsAll(Release candidate, Collection<Release> earlierReleases) {
        return earlierReleases.stream().allMatch(earlier -> Release.BY_VERSION_NUMBER.compare(candidate, earlier) > 0);
    }
}
