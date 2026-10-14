package it.uniroma2.isw2.storm.metrics;

import it.uniroma2.isw2.storm.git.Commit;

/**
 * Commit di massa: toccano più file di quasi tutti gli altri commit del progetto. Restano nel calcolo
 * delle metriche se creano classi Java (nuove funzionalità o codice importato), mentre quelli che non ne
 * creano sono riorganizzazioni meccaniche (rinomina dei package, intestazioni di licenza, spostamento in
 * moduli) e vengono esclusi da tutte le metriche, pur restando validi per seguire le rinomine delle classi.
 */
final class BulkCommits {

    static final int FILE_THRESHOLD = 50;

    private BulkCommits() {
    }

    static boolean isBulk(Commit commit) {
        return commit.changes().size() > FILE_THRESHOLD;
    }

    static boolean isMechanical(Commit commit) {
        return isBulk(commit) && !commit.createsJavaFile();
    }
}
