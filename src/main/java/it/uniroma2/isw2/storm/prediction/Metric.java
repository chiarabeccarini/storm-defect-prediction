package it.uniroma2.isw2.storm.prediction;

import java.util.Collection;
import java.util.function.ToDoubleFunction;

/** Le cinque metriche riportate e confrontate con il test statistico. */
public enum Metric {
    ACCURACY("Accuracy", PredictionMetrics::accuracy),
    PRECISION("Precision", PredictionMetrics::precision),
    RECALL("Recall", PredictionMetrics::recall),
    AUC("AUC", PredictionMetrics::auc),
    KAPPA("Kappa", PredictionMetrics::kappa);

    private final String label;
    private final ToDoubleFunction<PredictionMetrics> value;

    Metric(String label, ToDoubleFunction<PredictionMetrics> value) {
        this.label = label;
        this.value = value;
    }

    public String label() {
        return label;
    }

    public double of(PredictionMetrics metrics) {
        return value.applyAsDouble(metrics);
    }

    /** Per i log: tre decimali, oppure "non definita" per un valore NaN come la Precision di ZeroR. */
    public static String display(double value) {
        return Double.isNaN(value) ? "non definita" : String.format("%.3f", value);
    }

    // I valori non definiti (NaN) si escludono, come fa l'Experimenter con i valori mancanti.
    public double mean(Collection<PredictionMetrics> evaluations) {
        return evaluations.stream().mapToDouble(this::of).filter(metric -> !Double.isNaN(metric)).average()
                .orElse(Double.NaN);
    }
}
