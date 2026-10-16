package it.uniroma2.isw2.storm.smells;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conta le violazioni delle regole PMD di ogni classe con il set {@code quickstart}, la selezione raccomandata da
 * PMD: il catalogo completo segnalerebbe smell in quasi tutte le classi, soprattutto per regole di stile.
 */
public final class PmdSmellCounter {

    private static final String RULESET = "rulesets/java/quickstart.xml";

    private PmdSmellCounter() {
    }

    /** Smell di ciascuna classe, indicata con il percorso relativo alla copia di lavoro. */
    public static Map<String, Integer> countByClass(Path workTree, List<String> classPaths) {
        if (classPaths.isEmpty()) {
            throw new IllegalArgumentException("Nessuna classe da analizzare in " + workTree);
        }
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.addRuleSet(RULESET);
        configuration.setIgnoreIncrementalAnalysis(true);
        // Solo le classi del JDK: altrimenti PMD risolverebbe i tipi anche con le librerie di questo programma e i
        // conteggi potrebbero cambiare al variare delle sue dipendenze, invece che del codice di Storm.
        configuration.setClassLoader(ClassLoader.getPlatformClassLoader());
        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            classPaths.forEach(classPath -> analysis.files().addFile(workTree.resolve(classPath)));
            Report report = analysis.performAnalysisAndCollectReport();
            requireCompleteAnalysis(report);
            return smellsByClass(workTree, classPaths, report.getViolations());
        }
    }

    // Un file che PMD non riesce ad analizzare risulterebbe senza smell: meglio fermarsi che scrivere uno 0 falso.
    private static void requireCompleteAnalysis(Report report) {
        if (!report.getConfigurationErrors().isEmpty()) {
            throw new IllegalStateException("Configurazione di PMD non valida: " + report.getConfigurationErrors().get(0).issue());
        }
        if (!report.getProcessingErrors().isEmpty()) {
            Report.ProcessingError error = report.getProcessingErrors().get(0);
            throw new IllegalStateException("PMD non ha analizzato " + error.getFileId().getOriginalPath() + ": " + error.getMsg());
        }
    }

    private static Map<String, Integer> smellsByClass(Path workTree, List<String> classPaths, List<RuleViolation> violations) {
        Map<String, Integer> smells = new HashMap<>();
        classPaths.forEach(classPath -> smells.put(classPath, 0));
        for (RuleViolation violation : violations) {
            String classPath = relativePath(workTree, violation.getFileId());
            if (!smells.containsKey(classPath)) {
                throw new IllegalStateException("Violazione su un file non richiesto: " + classPath);
            }
            smells.merge(classPath, 1, Integer::sum);
        }
        return smells;
    }

    private static String relativePath(Path workTree, FileId fileId) {
        return workTree.toAbsolutePath().relativize(Path.of(fileId.getAbsolutePath())).toString().replace('\\', '/');
    }
}
