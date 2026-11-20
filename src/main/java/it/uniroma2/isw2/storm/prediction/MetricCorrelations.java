package it.uniroma2.isw2.storm.prediction;

import weka.core.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Correlazione di Pearson di ogni metrica grezza con Bugginess (Yes = 1, No = 0), nell'ordine delle colonne del
 * dataset: un valore positivo indica che la metrica tende a essere più alta nelle classi difettose.
 */
public final class MetricCorrelations {

    private MetricCorrelations() {
    }

    public record Correlation(String metric, double pearson) {
    }

    public static List<Correlation> withBugginess(PredictionDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset obbligatorio per le correlazioni");
        }
        double[] bugginess = dataset.positiveClassIndicator();
        requireVariation(bugginess, "Bugginess");
        List<String> metrics = dataset.metricNames();
        return IntStream.range(0, metrics.size())
                .mapToObj(metric -> new Correlation(metrics.get(metric),
                        pearson(dataset.rawValues(metric), bugginess, metrics.get(metric))))
                .toList();
    }

    // Utils.correlation restituisce 1 per una colonna costante, dove Pearson non è definito: si controlla prima.
    private static double pearson(double[] values, double[] bugginess, String metric) {
        requireVariation(values, metric);
        return Utils.correlation(values, bugginess, values.length);
    }

    private static void requireVariation(double[] values, String column) {
        if (Arrays.stream(values).distinct().count() < 2) {
            throw new IllegalStateException("Colonna " + column
                    + " costante: la correlazione di Pearson non è definita");
        }
    }
}
