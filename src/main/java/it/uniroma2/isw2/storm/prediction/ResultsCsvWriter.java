package it.uniroma2.isw2.storm.prediction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CSV dei risultati: per la cross-validation una riga per schema, ripetizione e fold e una riga per confronto
 * statistico; per lo split cronologico una riga per schema.
 */
public final class ResultsCsvWriter {

    private static final String EVALUATION_COLUMNS = "Training,Test,Attributes,Accuracy,Precision,Recall,AUC,Kappa,"
            + "TP,FP,TN,FN";
    private static final String FOLDS_HEADER = "Scheme,Run,Fold," + EVALUATION_COLUMNS;
    private static final String SPLIT_HEADER = "Scheme," + EVALUATION_COLUMNS;
    private static final String TESTS_HEADER = "Metric,Scheme,Reference,SchemeMean,ReferenceMean,PValue,Outcome";

    private ResultsCsvWriter() {
    }

    public static void writeFolds(List<FoldResult> results, Path csvFile) throws IOException {
        requireRows(results, csvFile);
        write(FOLDS_HEADER, results.stream()
                .map(result -> String.join(",", result.evaluation().scheme().name(), String.valueOf(result.run()),
                        String.valueOf(result.fold()), evaluationFields(result.evaluation())))
                .toList(), csvFile);
    }

    public static void writeSplit(List<SchemeEvaluation> evaluations, Path csvFile) throws IOException {
        requireRows(evaluations, csvFile);
        write(SPLIT_HEADER, evaluations.stream()
                .map(evaluation -> String.join(",", evaluation.scheme().name(), evaluationFields(evaluation)))
                .toList(), csvFile);
    }

    public static void writeComparisons(List<CrossValidationTests.Comparison> comparisons, Path csvFile)
            throws IOException {
        requireRows(comparisons, csvFile);
        write(TESTS_HEADER, comparisons.stream().map(ResultsCsvWriter::toCsvLine).toList(), csvFile);
    }

    private static void requireRows(List<?> rows, Path csvFile) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nessun risultato da scrivere in " + csvFile);
        }
    }

    private static void write(String header, List<String> rows, Path csvFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(rows);
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static String evaluationFields(SchemeEvaluation evaluation) {
        PredictionMetrics metrics = evaluation.metrics();
        PredictionMetrics.Confusion confusion = metrics.confusion();
        return String.join(",", String.valueOf(evaluation.trainingSize()), String.valueOf(evaluation.testSize()),
                String.valueOf(evaluation.attributes()), decimal(metrics.accuracy()), decimal(metrics.precision()),
                decimal(metrics.recall()), decimal(metrics.auc()), decimal(metrics.kappa()),
                String.valueOf(confusion.truePositives()), String.valueOf(confusion.falsePositives()),
                String.valueOf(confusion.trueNegatives()), String.valueOf(confusion.falseNegatives()));
    }

    private static String toCsvLine(CrossValidationTests.Comparison comparison) {
        return String.join(",", comparison.metric().label(), comparison.scheme().name(), comparison.reference().name(),
                decimal(comparison.schemeMean()), decimal(comparison.referenceMean()),
                String.valueOf(comparison.pValue()), comparison.outcome());
    }

    // Punto decimale qualunque sia la lingua del PC; i valori non definiti restano NaN.
    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
