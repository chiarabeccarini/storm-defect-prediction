package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.IOException;
import java.util.Set;

/**
 * Commit di una release individuati per ascendenza: la finestra contiene quelli raggiungibili dalla release
 * ma non dalla precedente, la storia tutti quelli raggiungibili. Per la prima release le due coincidono.
 */
record ReleaseCommits(Set<String> window, Set<String> history) {

    static ReleaseCommits of(GitRepository repository, IncludedRelease release, String previousReleaseCommit) throws IOException {
        Set<String> history = repository.commitsIn(release.commit());
        if (previousReleaseCommit == null) {
            return new ReleaseCommits(history, history);
        }
        return new ReleaseCommits(repository.commitsIn(previousReleaseCommit + ".." + release.commit()), history);
    }
}
