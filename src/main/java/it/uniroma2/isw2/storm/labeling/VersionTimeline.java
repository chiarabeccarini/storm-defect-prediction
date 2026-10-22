package it.uniroma2.isw2.storm.labeling;

import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.MainlineRelease;
import it.uniroma2.isw2.storm.release.Release;
import it.uniroma2.isw2.storm.ticket.Ticket;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Colloca i ticket sulla linea principale: Opening Version dalla data di apertura, Fixed Version dall'ascendenza
 * dei commit di fix e Injected Version dalle Affected Version, quando sono affidabili. Le versioni non datate
 * su Jira sono ignorate.
 */
final class VersionTimeline {

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)");

    private final List<MainlineRelease> mainline;
    private final Map<String, LocalDate> versionDates;
    private final Map<Integer, Set<String>> commitsByRelease;

    private VersionTimeline(List<MainlineRelease> mainline, Map<String, LocalDate> versionDates,
                            Map<Integer, Set<String>> commitsByRelease) {
        this.mainline = mainline;
        this.versionDates = versionDates;
        this.commitsByRelease = commitsByRelease;
    }

    static VersionTimeline load(GitRepository repository, List<MainlineRelease> mainline, List<Release> datedVersions)
            throws IOException {
        if (mainline.isEmpty() || datedVersions.isEmpty()) {
            throw new IllegalArgumentException("Release della linea principale o versioni datate mancanti");
        }
        List<MainlineRelease> chronological = mainline.stream()
                .sorted(Comparator.comparingInt(MainlineRelease::releaseId))
                .toList();
        Map<Integer, Set<String>> commitsByRelease = new LinkedHashMap<>();
        for (MainlineRelease release : chronological) {
            commitsByRelease.put(release.releaseId(), repository.commitsIn(release.commit()));
        }
        Map<String, LocalDate> versionDates = datedVersions.stream()
                .collect(Collectors.toUnmodifiableMap(Release::name, Release::releaseDate));
        return new VersionTimeline(chronological, versionDates, commitsByRelease);
    }

    /** Ultima release della linea principale rilasciata entro la data di apertura del ticket. */
    int openingVersion(Ticket ticket) {
        LocalDate opening = ticket.created().toLocalDate();
        return mainline.stream()
                .filter(release -> !release.releaseDate().isAfter(opening))
                .mapToInt(MainlineRelease::releaseId)
                .max()
                .orElseThrow(() -> new IllegalStateException("Ticket " + ticket.key() + " aperto prima della prima release"));
    }

    /**
     * Prima release della linea principale il cui tag contiene almeno un commit di fix: è lo stesso criterio
     * di ascendenza delle finestre delle metriche, invece della data di risoluzione.
     */
    OptionalInt fixedVersion(Set<String> fixCommits) {
        return commitsByRelease.entrySet().stream()
                .filter(release -> !Collections.disjoint(release.getValue(), fixCommits))
                .mapToInt(Map.Entry::getKey)
                .findFirst();
    }

    /** Fixed Version secondo la data di risoluzione: serve solo a contare i ticket per cui l'ascendenza la cambia. */
    OptionalInt fixedVersionByResolutionDate(Ticket ticket) {
        LocalDate resolution = ticket.resolved().toLocalDate();
        return mainline.stream()
                .filter(release -> !release.releaseDate().isBefore(resolution))
                .mapToInt(MainlineRelease::releaseId)
                .findFirst();
    }

    boolean hasDatedAffectedVersion(Ticket ticket) {
        return ticket.affectedVersions().stream().anyMatch(versionDates::containsKey);
    }

    /**
     * Injected Version letta dalle Affected Version: la più vecchia, ricondotta alla linea principale. Le Affected
     * Version sono affidabili solo se la più vecchia è uscita entro l'apertura del ticket, altrimenti il valore manca.
     */
    OptionalInt reliableInjectedVersion(Ticket ticket) {
        List<String> datedVersions = ticket.affectedVersions().stream().filter(versionDates::containsKey).toList();
        Optional<LocalDate> oldestDate = datedVersions.stream().map(versionDates::get).min(Comparator.naturalOrder());
        if (oldestDate.isEmpty() || oldestDate.get().isAfter(ticket.created().toLocalDate())) {
            return OptionalInt.empty();
        }
        return datedVersions.stream()
                .map(this::mainlineReleaseOf)
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .min();
    }

    // Una release di manutenzione è ricondotta alla release della linea principale da cui deriva il suo ramo:
    // la più recente con lo stesso major.minor e rilasciata non dopo di lei. Una release della linea principale
    // ricade su se stessa.
    private Optional<Integer> mainlineReleaseOf(String versionName) {
        LocalDate versionDate = versionDates.get(versionName);
        Optional<String> line = majorMinor(versionName);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        return mainline.stream()
                .filter(release -> line.equals(majorMinor(release.name())) && !release.releaseDate().isAfter(versionDate))
                .map(MainlineRelease::releaseId)
                .max(Comparator.naturalOrder());
    }

    private static Optional<String> majorMinor(String versionName) {
        Matcher matcher = MAJOR_MINOR.matcher(Objects.requireNonNull(versionName, "versionName"));
        return matcher.find() ? Optional.of(matcher.group(1) + "." + matcher.group(2)) : Optional.empty();
    }
}
