package it.uniroma2.isw2.storm.prediction;

import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Cross-validation stratificata ripetuta, con le stesse suddivisioni dell'Experimenter di Weka 3.8
 * (CrossValidationResultProducer): alla ripetizione r i dati si mescolano con il seed r, si stratificano e i fold si
 * prendono in ordine. Tutti gli schemi sono valutati sugli stessi fold, così il confronto è appaiato.
 */
public final class CrossValidation {

    private static final Logger LOGGER = Logger.getLogger(CrossValidation.class.getName());

    private final int repetitions;
    private final int folds;

    public CrossValidation(int repetitions, int folds) {
        if (repetitions < 1 || folds < 2) {
            throw new IllegalArgumentException("Servono almeno una ripetizione e due fold: " + repetitions + "x" + folds);
        }
        this.repetitions = repetitions;
        this.folds = folds;
    }

    public List<FoldResult> run(Instances dataset, List<PredictionScheme> schemes) {
        List<FoldResult> results = new ArrayList<>();
        for (int run = 1; run <= repetitions; run++) {
            Instances shuffled = new Instances(dataset);
            Random random = new Random(run);
            shuffled.randomize(random);
            shuffled.stratify(folds);
            for (int fold = 0; fold < folds; fold++) {
                Instances training = shuffled.trainCV(folds, fold, random);
                Instances test = shuffled.testCV(folds, fold);
                for (PredictionScheme scheme : schemes) {
                    results.add(new FoldResult(run, fold + 1, SchemeEvaluation.of(scheme, training, test)));
                }
            }
            int completed = run;
            LOGGER.info(() -> "Ripetizione " + completed + " di " + repetitions + " completata");
        }
        logSummary(results, schemes);
        return results;
    }

    // Una riga per schema con le medie sui fold: la riga di ZeroR è il controllo della baseline.
    private static void logSummary(List<FoldResult> results, List<PredictionScheme> schemes) {
        for (PredictionScheme scheme : schemes) {
            List<SchemeEvaluation> evaluations = results.stream()
                    .map(FoldResult::evaluation)
                    .filter(evaluation -> evaluation.scheme().equals(scheme))
                    .toList();
            List<PredictionMetrics> metrics = evaluations.stream().map(SchemeEvaluation::metrics).toList();
            double attributes = evaluations.stream().mapToInt(SchemeEvaluation::attributes).average().orElse(Double.NaN);
            LOGGER.info(() -> String.format("%s: Accuracy %.3f, Precision %s, Recall %.3f, AUC %.3f, Kappa %.3f, "
                            + "attributi usati %.1f (medie su %d fold)", scheme.name(), Metric.ACCURACY.mean(metrics),
                    formatDefined(Metric.PRECISION.mean(metrics)), Metric.RECALL.mean(metrics), Metric.AUC.mean(metrics),
                    Metric.KAPPA.mean(metrics), attributes, metrics.size()));
        }
    }

    private static String formatDefined(double value) {
        return Double.isNaN(value) ? "non definita" : String.format("%.3f", value);
    }
}
