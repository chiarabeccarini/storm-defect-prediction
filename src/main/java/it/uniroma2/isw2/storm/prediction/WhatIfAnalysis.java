package it.uniroma2.isw2.storm.prediction;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Analisi What-If: il BClassifier si addestra una sola volta su A e lo stesso modello predice A, B+, B e C. Si contano
 * le righe predette Yes senza guardare se la predizione è corretta; solo per A su A si riporta anche la matrice di
 * confusione, che misura il modello sui dati con cui è stato addestrato.
 */
public final class WhatIfAnalysis {

    private static final Logger LOGGER = Logger.getLogger(WhatIfAnalysis.class.getName());

    private WhatIfAnalysis() {
    }

    /** Righe di un dataset e classi Yes reali e predette; B è un dataset sintetico e non ha classi reali. */
    public record DatasetCount(String dataset, int instances, OptionalInt actualYes, int predictedYes) {
    }

    /** Conteggi dei quattro dataset, matrice di confusione di A su A e righe che cambiano verdetto tra B+ e B. */
    public record Result(DatasetCount full, DatasetCount withSmells, DatasetCount smellsRemoved,
                         DatasetCount withoutSmells, PredictionMetrics.Confusion fullConfusion, int turnedNo,
                         int turnedYes) {

        public List<DatasetCount> counts() {
            return List.of(full, withSmells, smellsRemoved, withoutSmells);
        }

        // Formule del corso: reali di B+ meno predette di B, rapportate alle reali di A e alle reali di B+.
        public int avoidable() {
            return withSmells.actualYes().orElseThrow() - smellsRemoved.predictedYes();
        }

        public double avoidableShareOfBugs() {
            return (double) avoidable() / full.actualYes().orElseThrow();
        }

        public double avoidableShareOfPreventable() {
            return (double) avoidable() / withSmells.actualYes().orElseThrow();
        }

        public int unavoidable() {
            return withoutSmells.actualYes().orElseThrow();
        }
    }

    /** I tre dataset ricavati da A arrivano come file riletti, così si predice proprio ciò che è stato salvato. */
    public static Result analyze(PredictionScheme scheme, Instances full, Instances withSmells, Instances smellsRemoved,
                                 Instances withoutSmells, String smellsAttribute) {
        WhatIfDatasets datasets = new WhatIfDatasets(full, withSmells, smellsRemoved, withoutSmells);
        int positive = SchemeEvaluation.positiveClass(full);
        requireConsistent(datasets, smellsAttribute, positive);
        Classifier model = trainedOn(scheme, full);
        boolean[] fullYes = predictedYes(model, full, positive);
        boolean[] withSmellsYes = predictedYes(model, datasets.withSmells(), positive);
        boolean[] removedYes = predictedYes(model, datasets.smellsRemoved(), positive);
        Result result = new Result(count("A", full, positive, fullYes),
                count("B+", datasets.withSmells(), positive, withSmellsYes),
                new DatasetCount("B", removedYes.length, OptionalInt.empty(), countTrue(removedYes)),
                count("C", datasets.withoutSmells(), positive, predictedYes(model, datasets.withoutSmells(), positive)),
                confusion(actualYes(full, positive), fullYes),
                rowsWhere(removedYes.length, row -> withSmellsYes[row] && !removedYes[row]),
                rowsWhere(removedYes.length, row -> !withSmellsYes[row] && removedYes[row]));
        requireSum("predette Yes", result.withSmells().predictedYes(), result.withoutSmells().predictedYes(),
                result.full().predictedYes());
        log(result);
        return result;
    }

    // Controlli prima dell'addestramento: se uno non torna, i conteggi non sarebbero confrontabili.
    private static void requireConsistent(WhatIfDatasets datasets, String smellsAttribute, int positive) {
        Instances full = datasets.full();
        for (Instances part : List.of(datasets.withSmells(), datasets.smellsRemoved(), datasets.withoutSmells())) {
            if (!full.equalHeaders(part)) {
                throw new IllegalStateException("Attributi diversi tra A e " + part.relationName() + ": "
                        + full.equalHeadersMsg(part));
            }
        }
        requireSum("righe", datasets.withSmells().numInstances(), datasets.withoutSmells().numInstances(),
                full.numInstances());
        int withSmellsYes = countTrue(actualYes(datasets.withSmells(), positive));
        int withoutSmellsYes = countTrue(actualYes(datasets.withoutSmells(), positive));
        requireSum("Yes reali", withSmellsYes, withoutSmellsYes, countTrue(actualYes(full, positive)));
        requireSmellsRemoved(datasets.withSmells(), datasets.smellsRemoved(),
                WhatIfDatasets.smellsIndex(full, smellsAttribute));
        LOGGER.info(() -> String.format("Controlli: righe B+ %d + C %d = A %d; Yes reali %d + %d = %d; stessi attributi "
                        + "e classe %s in A, B+, B e C; B uguale a B+ tranne %s (tutti 0)",
                datasets.withSmells().numInstances(), datasets.withoutSmells().numInstances(), full.numInstances(),
                withSmellsYes, withoutSmellsYes, withSmellsYes + withoutSmellsYes, classValues(full), smellsAttribute));
    }

    private static void requireSum(String quantity, int first, int second, int total) {
        if (first + second != total) {
            throw new IllegalStateException(quantity + ": B+ " + first + " + C " + second + " diverso da A " + total);
        }
    }

    // B deve coincidere con B+ riga per riga in ogni valore, tranne il numero di smell, che vale 0.
    private static void requireSmellsRemoved(Instances withSmells, Instances smellsRemoved, int smells) {
        if (withSmells.numInstances() != smellsRemoved.numInstances()) {
            throw new IllegalStateException("Righe diverse in B+ (" + withSmells.numInstances() + ") e in B ("
                    + smellsRemoved.numInstances() + ")");
        }
        for (int row = 0; row < withSmells.numInstances(); row++) {
            Instance original = withSmells.instance(row);
            Instance removed = smellsRemoved.instance(row);
            for (int attribute = 0; attribute < original.numAttributes(); attribute++) {
                double expected = attribute == smells ? 0 : original.value(attribute);
                if (Double.compare(removed.value(attribute), expected) != 0) {
                    throw new IllegalStateException("B diverso da B+ alla riga " + (row + 1) + ", attributo "
                            + original.attribute(attribute).name());
                }
            }
        }
    }

    private static Classifier trainedOn(PredictionScheme scheme, Instances full) {
        try {
            Classifier model = AbstractClassifier.makeCopy(scheme.template());
            model.buildClassifier(full);
            return model;
        } catch (Exception e) {
            // Le API di Weka dichiarano Exception generiche: si conserva la causa e si aggiunge il contesto.
            throw new IllegalStateException("Addestramento non riuscito per " + scheme.name(), e);
        }
    }

    // L'indice di Yes viene dal training: un ordine diverso dei valori della classe è già escluso dai controlli.
    private static boolean[] predictedYes(Classifier model, Instances dataset, int positive) {
        boolean[] predicted = new boolean[dataset.numInstances()];
        for (int row = 0; row < predicted.length; row++) {
            double predictedClass = classOf(model, dataset, row);
            if (Utils.isMissingValue(predictedClass)) {
                throw new IllegalStateException("Nessuna classe predetta alla riga " + (row + 1) + " di "
                        + dataset.relationName());
            }
            predicted[row] = (int) predictedClass == positive;
        }
        return predicted;
    }

    private static double classOf(Classifier model, Instances dataset, int row) {
        try {
            return model.classifyInstance(dataset.instance(row));
        } catch (Exception e) {
            // Le API di Weka dichiarano Exception generiche: si conserva la causa e si aggiunge il contesto.
            throw new IllegalStateException("Predizione non riuscita alla riga " + (row + 1) + " di "
                    + dataset.relationName(), e);
        }
    }

    private static boolean[] actualYes(Instances dataset, int positive) {
        boolean[] actual = new boolean[dataset.numInstances()];
        for (int row = 0; row < actual.length; row++) {
            actual[row] = (int) dataset.instance(row).classValue() == positive;
        }
        return actual;
    }

    private static DatasetCount count(String name, Instances dataset, int positive, boolean[] predicted) {
        return new DatasetCount(name, dataset.numInstances(), OptionalInt.of(countTrue(actualYes(dataset, positive))),
                countTrue(predicted));
    }

    private static PredictionMetrics.Confusion confusion(boolean[] actual, boolean[] predicted) {
        return new PredictionMetrics.Confusion(rowsWhere(actual.length, row -> predicted[row] && actual[row]),
                rowsWhere(actual.length, row -> predicted[row] && !actual[row]),
                rowsWhere(actual.length, row -> !predicted[row] && !actual[row]),
                rowsWhere(actual.length, row -> !predicted[row] && actual[row]));
    }

    private static int countTrue(boolean[] values) {
        return rowsWhere(values.length, row -> values[row]);
    }

    private static int rowsWhere(int rows, IntPredicate condition) {
        return Math.toIntExact(IntStream.range(0, rows).filter(condition).count());
    }

    private static String classValues(Instances dataset) {
        return IntStream.range(0, dataset.classAttribute().numValues())
                .mapToObj(dataset.classAttribute()::value)
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static void log(Result result) {
        PredictionMetrics.Confusion confusion = result.fullConfusion();
        LOGGER.info(() -> String.format("A su A (valutazione sui dati di addestramento): reali %d, predette %d; TP %d, "
                        + "FP %d, TN %d, FN %d", result.full().actualYes().orElseThrow(), result.full().predictedYes(),
                confusion.truePositives(), confusion.falsePositives(), confusion.trueNegatives(),
                confusion.falseNegatives()));
        LOGGER.info(() -> String.format("B+: %d istanze, reali %d, predette %d; B: %d istanze, predette %d; C: %d istanze, "
                        + "reali %d, predette %d", result.withSmells().instances(),
                result.withSmells().actualYes().orElseThrow(), result.withSmells().predictedYes(),
                result.smellsRemoved().instances(), result.smellsRemoved().predictedYes(), result.withoutSmells().instances(),
                result.withoutSmells().actualYes().orElseThrow(), result.withoutSmells().predictedYes()));
        LOGGER.info(() -> String.format("Controllo: predette di A %d = predette di B+ %d + predette di C %d",
                result.full().predictedYes(), result.withSmells().predictedYes(), result.withoutSmells().predictedYes()));
        LOGGER.info(() -> String.format("Da B+ a B: %d righe passano da Yes a No, %d da No a Yes (differenza netta %d)",
                result.turnedNo(), result.turnedYes(), result.turnedNo() - result.turnedYes()));
        LOGGER.info(() -> String.format("Bug evitabili in totale: reali di B+ %d - predette di B %d = %d; in proporzione "
                        + "%.1f%% delle reali di A (%d); tra le prevenibili %.1f%% delle reali di B+; inevitabili "
                        + "(reali di C) %d", result.withSmells().actualYes().orElseThrow(),
                result.smellsRemoved().predictedYes(), result.avoidable(), 100 * result.avoidableShareOfBugs(),
                result.full().actualYes().orElseThrow(), 100 * result.avoidableShareOfPreventable(),
                result.unavoidable()));
    }
}
