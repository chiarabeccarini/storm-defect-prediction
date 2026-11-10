package it.uniroma2.isw2.storm.prediction;

import weka.core.Instances;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Split cronologico: ogni schema si addestra una sola volta sul file di training (le release precedenti) e si valuta
 * sul file di test (l'ultima release), che nessun filtro tocca.
 */
public final class ChronologicalSplit {

    private static final Logger LOGGER = Logger.getLogger(ChronologicalSplit.class.getName());

    private ChronologicalSplit() {
    }

    public static List<SchemeEvaluation> run(Instances training, Instances test, List<PredictionScheme> schemes) {
        if (!training.equalHeaders(test)) {
            throw new IllegalArgumentException("Training e test con attributi diversi: " + training.equalHeadersMsg(test));
        }
        List<String> selected = Configuration.selectedMetrics(training);
        LOGGER.info(() -> "Metriche scelte dalla feature selection sul training (" + selected.size() + "): "
                + String.join(", ", selected));
        List<SchemeEvaluation> evaluations = schemes.stream()
                .map(scheme -> SchemeEvaluation.of(scheme, training, test))
                .toList();
        evaluations.forEach(ChronologicalSplit::log);
        logBestBase(evaluations);
        return evaluations;
    }

    private static void log(SchemeEvaluation evaluation) {
        PredictionMetrics metrics = evaluation.metrics();
        PredictionMetrics.Confusion confusion = metrics.confusion();
        LOGGER.info(() -> String.format("%s: Accuracy %.3f, Precision %s, Recall %.3f, AUC %.3f, Kappa %.3f, "
                        + "attributi usati %d; TP %d, FP %d, TN %d, FN %d", evaluation.scheme().name(),
                metrics.accuracy(), Metric.display(metrics.precision()), metrics.recall(), metrics.auc(), metrics.kappa(),
                evaluation.attributes(), confusion.truePositives(), confusion.falsePositives(),
                confusion.trueNegatives(), confusion.falseNegatives()));
    }

    // Tra le configurazioni base decide il Kappa: serve a verificare che il vincitore coincida con quello della CV.
    private static void logBestBase(List<SchemeEvaluation> evaluations) {
        evaluations.stream()
                .filter(evaluation -> evaluation.scheme().configuration() == Configuration.BASE)
                .filter(evaluation -> !evaluation.scheme().classifier().equals(PredictionScheme.BASELINE))
                .max(Comparator.comparingDouble(evaluation -> evaluation.metrics().kappa()))
                .ifPresent(best -> LOGGER.info(() -> String.format("Kappa massimo sullo split tra le configurazioni "
                        + "base: %s (%.3f)", best.scheme().name(), best.metrics().kappa())));
    }
}
