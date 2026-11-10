package it.uniroma2.isw2.storm.prediction;

import weka.experiment.PairedStats;
import weka.experiment.PairedStatsCorrected;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test t appaiato corretto sui fold della cross-validation, con PairedStatsCorrected di Weka: è la statistica del
 * Paired T-Tester (corrected) dell'Experimenter, che con il rapporto tra istanze di test e di training tiene conto
 * della sovrapposizione dei training dei fold. Confronti: ogni classificatore di base contro ZeroR, i classificatori
 * di base tra loro, ogni configurazione contro la base dello stesso classificatore; ciascuno su tutte le metriche.
 */
public final class CrossValidationTests {

    private static final Logger LOGGER = Logger.getLogger(CrossValidationTests.class.getName());

    private CrossValidationTests() {
    }

    /** Significance: 1 se lo schema è migliore del riferimento al livello indicato, -1 se è peggiore, 0 se pari. */
    public record Comparison(Metric metric, PredictionScheme scheme, PredictionScheme reference, double schemeMean,
                             double referenceMean, double pValue, int significance) {

        public String outcome() {
            if (Double.isNaN(pValue)) {
                return "non definito";
            }
            if (significance > 0) {
                return "migliore";
            }
            return significance < 0 ? "peggiore" : "pari";
        }
    }

    private record Pair(PredictionScheme scheme, PredictionScheme reference) {
    }

    public static List<Comparison> compare(List<FoldResult> results, List<PredictionScheme> schemes,
                                           double significanceLevel) {
        List<Comparison> comparisons = new ArrayList<>();
        for (Pair pair : pairs(schemes)) {
            for (Metric metric : Metric.values()) {
                comparisons.add(compare(metric, pair, results, significanceLevel));
            }
        }
        logKappa(comparisons);
        return comparisons;
    }

    private static List<Pair> pairs(List<PredictionScheme> schemes) {
        PredictionScheme baseline = schemes.stream()
                .filter(scheme -> scheme.classifier().equals(PredictionScheme.BASELINE))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Manca la baseline " + PredictionScheme.BASELINE));
        List<PredictionScheme> bases = schemes.stream()
                .filter(scheme -> scheme != baseline && scheme.configuration() == Configuration.BASE)
                .toList();
        List<Pair> pairs = new ArrayList<>();
        bases.forEach(base -> pairs.add(new Pair(base, baseline)));
        for (int first = 0; first < bases.size(); first++) {
            for (int second = first + 1; second < bases.size(); second++) {
                pairs.add(new Pair(bases.get(first), bases.get(second)));
            }
        }
        schemes.stream()
                .filter(scheme -> scheme.configuration() != Configuration.BASE)
                .forEach(scheme -> pairs.add(new Pair(scheme, baseOf(scheme, bases))));
        return pairs;
    }

    private static PredictionScheme baseOf(PredictionScheme scheme, List<PredictionScheme> bases) {
        return bases.stream()
                .filter(base -> base.classifier().equals(scheme.classifier()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Manca la configurazione base di " + scheme.classifier()));
    }

    // I fold con un valore non definito (NaN) si escludono, come fa l'Experimenter con i valori mancanti.
    private static Comparison compare(Metric metric, Pair pair, List<FoldResult> results, double significanceLevel) {
        List<FoldResult> schemeResults = resultsOf(pair.scheme(), results);
        List<FoldResult> referenceResults = resultsOf(pair.reference(), results);
        if (schemeResults.isEmpty() || schemeResults.size() != referenceResults.size()) {
            throw new IllegalStateException("Fold diversi per " + pair.scheme().name() + " e " + pair.reference().name());
        }
        double testInstances = schemeResults.stream().mapToDouble(result -> result.evaluation().testSize()).sum();
        double trainingInstances = schemeResults.stream().mapToDouble(result -> result.evaluation().trainingSize()).sum();
        PairedStats stats = new PairedStatsCorrected(significanceLevel, testInstances / trainingInstances);
        for (int index = 0; index < schemeResults.size(); index++) {
            FoldResult schemeFold = schemeResults.get(index);
            FoldResult referenceFold = referenceResults.get(index);
            if (schemeFold.run() != referenceFold.run() || schemeFold.fold() != referenceFold.fold()) {
                throw new IllegalStateException("Fold non appaiati alla posizione " + index);
            }
            double schemeValue = metric.of(schemeFold.evaluation().metrics());
            double referenceValue = metric.of(referenceFold.evaluation().metrics());
            if (!Double.isNaN(schemeValue) && !Double.isNaN(referenceValue)) {
                stats.add(schemeValue, referenceValue);
            }
        }
        if (stats.count < 2) {
            return new Comparison(metric, pair.scheme(), pair.reference(), Double.NaN, Double.NaN, Double.NaN, 0);
        }
        stats.calculateDerived();
        return new Comparison(metric, pair.scheme(), pair.reference(), stats.xStats.mean, stats.yStats.mean,
                stats.differencesProbability, stats.differencesSignificance);
    }

    private static List<FoldResult> resultsOf(PredictionScheme scheme, List<FoldResult> results) {
        return results.stream().filter(result -> result.evaluation().scheme().equals(scheme)).toList();
    }

    // Nei log solo il Kappa, la metrica che decide il BClassifier; tutte le metriche sono nel CSV dei test.
    private static void logKappa(List<Comparison> comparisons) {
        comparisons.stream()
                .filter(comparison -> comparison.metric() == Metric.KAPPA)
                .forEach(comparison -> LOGGER.info(() -> String.format("Kappa, %s contro %s: %.3f contro %.3f, p %s, %s",
                        comparison.scheme().name(), comparison.reference().name(), comparison.schemeMean(),
                        comparison.referenceMean(), formatPValue(comparison.pValue()), comparison.outcome())));
    }

    private static String formatPValue(double pValue) {
        if (Double.isNaN(pValue)) {
            return "non definito";
        }
        return pValue < 0.0001 ? "< 0,0001" : String.format("= %.4f", pValue);
    }
}
