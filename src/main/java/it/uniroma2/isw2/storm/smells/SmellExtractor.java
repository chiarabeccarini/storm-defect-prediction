package it.uniroma2.isw2.storm.smells;

import it.uniroma2.isw2.storm.classes.ReleaseClass;
import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Conta gli smell delle classi di ogni release sull'ultimo commit della release, facendone il checkout. */
public final class SmellExtractor {

    private static final Logger LOGGER = Logger.getLogger(SmellExtractor.class.getName());

    private final GitRepository repository;

    public SmellExtractor(GitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<ClassSmells> extract(List<IncludedRelease> releases, List<ReleaseClass> releaseClasses) throws IOException {
        if (releases.isEmpty() || releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Release o classi mancanti per il conteggio degli smell");
        }
        Map<Integer, List<String>> classesByRelease = releaseClasses.stream().collect(Collectors.groupingBy(
                ReleaseClass::releaseId, Collectors.mapping(ReleaseClass::path, Collectors.toList())));
        String originalBranch = repository.currentBranch();
        List<ClassSmells> rows = new ArrayList<>();
        try {
            for (IncludedRelease release : releases) {
                rows.addAll(releaseSmells(release, classesByRelease.getOrDefault(release.releaseId(), List.of())));
            }
        } finally {
            repository.checkout(originalBranch);
        }
        return rows;
    }

    private List<ClassSmells> releaseSmells(IncludedRelease release, List<String> classPaths) throws IOException {
        repository.checkout(release.commit());
        Map<String, Integer> smells = PmdSmellCounter.countByClass(repository.workTree(), classPaths);
        List<ClassSmells> rows = classPaths.stream()
                .map(classPath -> new ClassSmells(release.releaseId(), classPath, smells.get(classPath)))
                .toList();
        long cleanClasses = rows.stream().filter(row -> row.smells() == 0).count();
        int totalSmells = rows.stream().mapToInt(ClassSmells::smells).sum();
        LOGGER.info(() -> "Release " + release.name() + ": " + totalSmells + " smell su " + rows.size() + " classi, "
                + cleanClasses + " classi senza smell");
        return rows;
    }
}
