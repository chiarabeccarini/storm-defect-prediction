package it.uniroma2.isw2.storm.release;

import it.uniroma2.isw2.storm.git.GitRepository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TagResolver {

    private final GitRepository repository;

    public TagResolver(GitRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** In Storm i tag sono di norma {@code v<nome>} (es. v0.9.3), con eccezioni senza prefisso (es. 0.9.0.1). */
    public ResolvedTag resolve(Release release) throws IOException {
        Objects.requireNonNull(release, "release");
        for (String tagName : List.of("v" + release.name(), release.name())) {
            Optional<String> commit = repository.commitOfTag(tagName);
            if (commit.isPresent()) {
                return new ResolvedTag(tagName, commit.get());
            }
        }
        throw new IllegalStateException("Nessun tag Git per la release " + release.name());
    }

    public record ResolvedTag(String name, String commit) {
    }
}
