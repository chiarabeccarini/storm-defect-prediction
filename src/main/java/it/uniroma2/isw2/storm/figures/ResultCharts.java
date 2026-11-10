package it.uniroma2.isw2.storm.figures;

import it.uniroma2.isw2.storm.prediction.Metric;
import it.uniroma2.isw2.storm.prediction.PredictionScheme;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BoxChart;
import org.knowm.xchart.BoxChartBuilder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.internal.chartpart.Chart;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Grafici della Milestone 2, ricavati dal CSV dei fold della cross-validation e salvati in PNG con XChart.
 * I valori non definiti (NaN) si escludono, come nelle medie dei log.
 */
public final class ResultCharts {

    private static final Logger LOGGER = Logger.getLogger(ResultCharts.class.getName());
    private static final int BOXPLOT_WIDTH = 500;
    private static final int BAR_CHART_WIDTH = 800;
    private static final int HEIGHT = 400;
    // Risoluzione adatta alla stampa del report.
    private static final int DPI = 200;

    private ResultCharts() {
    }

    /**
     * Un boxplot per schema: la scatola va dal primo al terzo quartile con la mediana, i baffi arrivano a 1,5 volte
     * l'intervallo interquartile (limitati al minimo e al massimo) e i punti oltre i baffi sono disegnati a parte.
     */
    public static void boxplot(Map<String, Map<Metric, List<Double>>> folds, List<PredictionScheme> schemes,
                               Metric metric, Path pngFile) throws IOException {
        BoxChart chart = new BoxChartBuilder().width(BOXPLOT_WIDTH).height(HEIGHT).title(metric.label())
                .yAxisTitle(metric.label()).build();
        chart.getStyler().setLegendVisible(false);
        List<String> medians = new ArrayList<>();
        for (PredictionScheme scheme : schemes) {
            List<Double> values = definedValues(folds, scheme, metric);
            chart.addSeries(scheme.classifier(), values);
            medians.add(String.format("%s %.3f", scheme.classifier(), median(values)));
        }
        save(chart, pngFile);
        LOGGER.info(() -> pngFile + ": mediane " + String.join(", ", medians));
    }

    /** Barre delle medie sui fold, un gruppo per configurazione e una barra per metrica. */
    public static void configurationBars(Map<String, Map<Metric, List<Double>>> folds, List<PredictionScheme> schemes,
                                         List<Metric> metrics, Path pngFile) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(BAR_CHART_WIDTH).height(HEIGHT)
                .title(schemes.get(0).classifier() + ", medie sui fold della cross-validation").yAxisTitle("Valore")
                .build();
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(1.0);
        List<String> configurations = schemes.stream().map(scheme -> scheme.configuration().label()).toList();
        for (Metric metric : metrics) {
            List<Double> means = schemes.stream().map(scheme -> mean(definedValues(folds, scheme, metric))).toList();
            chart.addSeries(metric.label(), configurations, means);
        }
        save(chart, pngFile);
        for (PredictionScheme scheme : schemes) {
            List<String> values = metrics.stream()
                    .map(metric -> String.format("%s %.3f", metric.label(), mean(definedValues(folds, scheme, metric))))
                    .toList();
            LOGGER.info(() -> pngFile + ", " + scheme.configuration().label() + ": " + String.join(", ", values));
        }
    }

    private static List<Double> definedValues(Map<String, Map<Metric, List<Double>>> folds, PredictionScheme scheme,
                                              Metric metric) {
        Map<Metric, List<Double>> schemeFolds = folds.get(scheme.name());
        if (schemeFolds == null) {
            throw new IllegalArgumentException("Nessun risultato per " + scheme.name());
        }
        List<Double> values = schemeFolds.get(metric).stream().filter(value -> !value.isNaN()).toList();
        if (values.isEmpty()) {
            throw new IllegalArgumentException(metric.label() + " non definita in tutti i fold di " + scheme.name());
        }
        return values;
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    // Sfondo bianco invece del grigio del tema predefinito, per la stampa del report.
    private static void save(Chart<?, ?> chart, Path pngFile) throws IOException {
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        Files.createDirectories(pngFile.toAbsolutePath().getParent());
        BitmapEncoder.saveBitmapWithDPI(chart, pngFile.toString(), BitmapEncoder.BitmapFormat.PNG, DPI);
    }
}
