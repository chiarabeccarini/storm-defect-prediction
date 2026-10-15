package it.uniroma2.isw2.storm.metrics;

/**
 * Metriche di un commit calcolate sui soli file .java che modifica; la stessa struttura raccoglie anche la loro
 * somma sui commit che toccano una classe in una release.
 */
public record CommitMetrics(int subsystems, int directories, int files, double entropy, int addedLines, int deletedLines,
                            int developers, double ageWeeks, int uniqueChanges, int experience, double recentExperience,
                            int subsystemExperience) {

    static final CommitMetrics ZERO = new CommitMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    CommitMetrics plus(CommitMetrics other) {
        return new CommitMetrics(subsystems + other.subsystems, directories + other.directories, files + other.files,
                entropy + other.entropy, addedLines + other.addedLines, deletedLines + other.deletedLines,
                developers + other.developers, ageWeeks + other.ageWeeks, uniqueChanges + other.uniqueChanges,
                experience + other.experience, recentExperience + other.recentExperience,
                subsystemExperience + other.subsystemExperience);
    }
}
