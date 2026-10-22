package it.uniroma2.isw2.storm.labeling;

import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.git.Commit;
import it.uniroma2.isw2.storm.git.FileChange;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.history.ClassIdentityTracker;
import it.uniroma2.isw2.storm.metrics.FixCommitDetector;
import it.uniroma2.isw2.storm.release.MainlineRelease;
import it.uniroma2.isw2.storm.release.Release;
import it.uniroma2.isw2.storm.ticket.Ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Trova le classi difettose delle release incluse: per ogni ticket di bug le classi toccate dai suoi commit di fix
 * lo sono dalla Injected Version alla Fixed Version esclusa. I commit sono cercati su tutti i ref, perché diverse
 * release sono uscite da rami che non fanno parte di master; i merge collegano il ticket e contano per la Fixed
 * Version, ma le classi vengono solo dai commit che non sono merge, come nelle metriche.
 */
public final class BugginessExtractor {

    private static final Logger LOGGER = Logger.getLogger(BugginessExtractor.class.getName());
    private static final List<String> ALL_REFS = List.of("--all");

    private final GitRepository repository;
    private final FixCommitDetector fixDetector;

    public BugginessExtractor(GitRepository repository, FixCommitDetector fixDetector) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.fixDetector = Objects.requireNonNull(fixDetector, "fixDetector");
    }

    public Set<ReleaseClass> buggyClasses(List<MainlineRelease> mainline, List<Release> datedVersions, List<Ticket> tickets,
                                          List<ReleaseClass> releaseClasses) throws IOException {
        if (tickets.isEmpty() || releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Ticket o classi mancanti per l'etichettatura");
        }
        List<Commit> history = repository.history(ALL_REFS);
        ClassIdentityTracker identities = new ClassIdentityTracker(history);
        Map<String, Set<String>> fixCommits = fixCommitsByTicket(Stream.concat(history.stream(),
                repository.merges(ALL_REFS).stream()).toList());
        VersionTimeline timeline = VersionTimeline.load(repository, mainline, datedVersions);
        logLinkage(tickets, fixCommits.size(), timeline);

        Map<Ticket, Integer> fixedVersions = new LinkedHashMap<>();
        tickets.forEach(ticket -> timeline.fixedVersion(fixCommits.getOrDefault(ticket.key(), Set.of()))
                .ifPresent(fixedVersion -> fixedVersions.put(ticket, fixedVersion)));
        logFixedVersions(fixedVersions, fixCommits.size(), timeline);

        Set<ReleaseClass> buggyClasses = labeledClasses(lifecycles(fixedVersions, timeline),
                fixedClassesByTicket(history, identities), releaseClasses, identities);
        LOGGER.info(() -> String.format("Righe difettose: %d su %d (%.1f%%)", buggyClasses.size(), releaseClasses.size(),
                100.0 * buggyClasses.size() / releaseClasses.size()));
        return buggyClasses;
    }

    private Map<String, Set<String>> fixCommitsByTicket(List<Commit> commits) {
        Map<String, Set<String>> fixCommits = new HashMap<>();
        for (Commit commit : commits) {
            fixDetector.fixedTickets(commit.message())
                    .forEach(ticket -> fixCommits.computeIfAbsent(ticket, key -> new HashSet<>()).add(commit.hash()));
        }
        return fixCommits;
    }

    // Unione delle classi toccate da tutti i commit del ticket, ricondotte alla loro identità attraverso le rinomine.
    private Map<String, Set<String>> fixedClassesByTicket(List<Commit> history, ClassIdentityTracker identities) {
        Map<String, Set<String>> fixedClasses = new HashMap<>();
        for (Commit commit : history) {
            Set<String> classIdentities = commit.changes().stream()
                    .filter(FileChange::isJavaFile)
                    .map(change -> identities.identityOf(change.newPath()))
                    .collect(Collectors.toSet());
            fixDetector.fixedTickets(commit.message())
                    .forEach(ticket -> fixedClasses.computeIfAbsent(ticket, key -> new HashSet<>()).addAll(classIdentities));
        }
        return fixedClasses;
    }

    // P si calcola sui soli ticket con Injected Version nota; per gli altri la Injected Version si stima con P.
    private static List<TicketLifecycle> lifecycles(Map<Ticket, Integer> fixedVersions, VersionTimeline timeline) {
        List<TicketLifecycle> known = new ArrayList<>();
        Map<Ticket, Integer> unknown = new LinkedHashMap<>();
        fixedVersions.forEach((ticket, fixedVersion) -> {
            OptionalInt injectedVersion = timeline.reliableInjectedVersion(ticket);
            if (injectedVersion.isEmpty()) {
                unknown.put(ticket, fixedVersion);
                return;
            }
            known.add(new TicketLifecycle(ticket.key(), timeline.openingVersion(ticket), fixedVersion, injectedVersion.getAsInt()));
        });
        Proportion proportion = Proportion.of(known);
        List<TicketLifecycle> lifecycles = new ArrayList<>(known);
        unknown.forEach((ticket, fixedVersion) -> {
            int openingVersion = timeline.openingVersion(ticket);
            lifecycles.add(new TicketLifecycle(ticket.key(), openingVersion, fixedVersion,
                    proportion.estimateInjectedVersion(openingVersion, fixedVersion)));
        });
        logProportion(proportion, lifecycles.subList(known.size(), lifecycles.size()));
        return lifecycles;
    }

    private static Set<ReleaseClass> labeledClasses(List<TicketLifecycle> lifecycles, Map<String, Set<String>> fixedClasses,
                                                    List<ReleaseClass> releaseClasses, ClassIdentityTracker identities) {
        Map<Integer, Map<String, List<String>>> pathsByIdentity = releaseClasses.stream().collect(Collectors.groupingBy(
                ReleaseClass::releaseId, Collectors.groupingBy(releaseClass -> identities.identityOf(releaseClass.path()),
                        Collectors.mapping(ReleaseClass::path, Collectors.toList()))));
        Set<ReleaseClass> buggy = new HashSet<>();
        for (TicketLifecycle lifecycle : lifecycles) {
            Set<String> classIdentities = fixedClasses.getOrDefault(lifecycle.key(), Set.of());
            for (int releaseId = lifecycle.injectedVersion(); releaseId < lifecycle.fixedVersion(); releaseId++) {
                buggy.addAll(releaseClassesOf(releaseId, classIdentities, pathsByIdentity.getOrDefault(releaseId, Map.of())));
            }
        }
        return buggy;
    }

    private static List<ReleaseClass> releaseClassesOf(int releaseId, Set<String> classIdentities,
                                                       Map<String, List<String>> releasePathsByIdentity) {
        return classIdentities.stream()
                .flatMap(identity -> releasePathsByIdentity.getOrDefault(identity, List.of()).stream())
                .map(path -> new ReleaseClass(releaseId, path))
                .toList();
    }

    private static void logLinkage(List<Ticket> tickets, int linkedTickets, VersionTimeline timeline) {
        long reliable = tickets.stream().filter(ticket -> timeline.reliableInjectedVersion(ticket).isPresent()).count();
        long dated = tickets.stream().filter(timeline::hasDatedAffectedVersion).count();
        LOGGER.info(() -> "Ticket di bug: " + tickets.size() + "; Affected Version affidabili: " + reliable
                + ", non consistenti: " + (dated - reliable) + ", assenti o non datate: " + (tickets.size() - dated));
        LOGGER.info(() -> String.format("Linkage: %d ticket su %d (%.1f%%) hanno almeno un commit collegato",
                linkedTickets, tickets.size(), 100.0 * linkedTickets / tickets.size()));
    }

    private static void logFixedVersions(Map<Ticket, Integer> fixedVersions, int linkedTickets, VersionTimeline timeline) {
        long differentFromResolutionDate = fixedVersions.entrySet().stream()
                .filter(ticket -> !timeline.fixedVersionByResolutionDate(ticket.getKey()).equals(OptionalInt.of(ticket.getValue())))
                .count();
        long notAfterOpening = fixedVersions.entrySet().stream()
                .filter(ticket -> ticket.getValue() <= timeline.openingVersion(ticket.getKey()))
                .count();
        LOGGER.info(() -> "Ticket con Fixed Version: " + fixedVersions.size() + ", di cui " + differentFromResolutionDate
                + " diversa da quella data dalla data di risoluzione e " + notAfterOpening + " non successiva alla Opening "
                + "Version; ticket con commit non contenuti in nessuna release della linea principale: "
                + (linkedTickets - fixedVersions.size()));
    }

    private static void logProportion(Proportion proportion, List<TicketLifecycle> estimated) {
        long boundedToFirst = estimated.stream()
                .filter(lifecycle -> proportion.unboundedEstimate(lifecycle.openingVersion(), lifecycle.fixedVersion()) < 1)
                .count();
        LOGGER.info(() -> String.format("P = %.3f (mediana %.3f) su %d ticket", proportion.mean(), proportion.median(),
                proportion.tickets()));
        LOGGER.info(() -> "Injected Version stimate con Proportion: " + estimated.size() + ", di cui riportate alla release 1: "
                + boundedToFirst);
    }
}
