package it.uniroma2.isw2.storm.history;

import it.uniroma2.isw2.storm.git.Commit;
import it.uniroma2.isw2.storm.git.FileChange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Riconduce i percorsi di una stessa classe a un'unica identità attraverso le rinomine
 * rilevate da git: l'identità è il primo percorso con cui la classe compare nella storia.
 */
public final class ClassIdentityTracker {

    private final Map<String, String> identityByPath = new HashMap<>();

    /** I commit vanno forniti dal più vecchio al più recente, così una rinomina trova già l'identità del vecchio percorso. */
    public ClassIdentityTracker(List<Commit> chronologicalCommits) {
        for (Commit commit : chronologicalCommits) {
            commit.changes().stream()
                    .filter(FileChange::isRename)
                    .forEach(rename -> identityByPath.put(rename.newPath(), identityOf(rename.oldPath())));
        }
    }

    public String identityOf(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Percorso mancante");
        }
        return identityByPath.getOrDefault(path, path);
    }

    public long renamedPaths() {
        return identityByPath.size();
    }
}
