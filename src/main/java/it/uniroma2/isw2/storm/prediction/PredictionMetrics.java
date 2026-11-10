package it.uniroma2.isw2.storm.prediction;

import weka.classifiers.Evaluation;

/**
 * Metriche di una valutazione. Precision, Recall e AUC si riferiscono alla classe positiva (Yes); l'accuratezza è una
 * frazione tra 0 e 1. La Precision è NaN quando il classificatore non predice mai Yes, come ZeroR: 0 diviso 0.
 */
public record PredictionMetrics(double accuracy, double precision, double recall, double auc, double kappa,
                                Confusion confusion) {

    public record Confusion(long truePositives, long falsePositives, long trueNegatives, long falseNegatives) {
    }

    public static PredictionMetrics of(Evaluation evaluation, int positiveClass) {
        Confusion confusion = new Confusion(Math.round(evaluation.numTruePositives(positiveClass)),
                Math.round(evaluation.numFalsePositives(positiveClass)),
                Math.round(evaluation.numTrueNegatives(positiveClass)),
                Math.round(evaluation.numFalseNegatives(positiveClass)));
        return new PredictionMetrics(evaluation.pctCorrect() / 100, evaluation.precision(positiveClass),
                evaluation.recall(positiveClass), evaluation.areaUnderROC(positiveClass), evaluation.kappa(), confusion);
    }
}
