package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.git.GitRepository;
import it.uniroma2.isw2.storm.release.IncludedRelease;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Commit delle release individuati per ascendenza. La finestra contiene quelli raggiungibili dalla release ma non
 * dalla precedente; la storia è l'unione delle finestre dalla prima release, cioè i commit raggiungibili da almeno
 * una release fino a quella corrente, così il totale non perde i commit di una release precedente sviluppata su un
 * altro ramo. Per la prima release finestra e storia coincidono.
 */
record ReleaseCommits(Set<String> window, Set<String> history) {

    static Map<Integer, ReleaseCommits> byRelease(GitRepository repository, List<IncludedRelease> releases) throws IOException {
        Map<Integer, ReleaseCommits> commitsByRelease = new HashMap<>();
        Set<String> history = new HashSet<>();
        String previousCommit = null;
        for (IncludedRelease release : releases) {
            Set<String> reachable = repository.commitsIn(release.commit());
            Set<String> window = previousCommit == null ? reachable : repository.commitsIn(previousCommit + ".." + release.commit());
            history.addAll(reachable);
            commitsByRelease.put(release.releaseId(), new ReleaseCommits(window, Set.copyOf(history)));
            previousCommit = release.commit();
        }
        return commitsByRelease;
    }
}
