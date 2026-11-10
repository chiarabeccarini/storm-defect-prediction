package it.uniroma2.isw2.storm.prediction;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;

/**
 * Valutazione di uno schema su una coppia training-test, con gli stessi passaggi dell'Experimenter di Weka 3.8
 * (ClassifierSplitEvaluator): copia nuova del modello, addestramento sul training, valutazione sul test.
 * Gli attributi sono quelli che il classificatore usa davvero: meno di 44 se c'è la feature selection.
 */
public record SchemeEvaluation(PredictionScheme scheme, int trainingSize, int testSize, int attributes,
                               PredictionMetrics metrics) {

    public static SchemeEvaluation of(PredictionScheme scheme, Instances training, Instances test) {
        try {
            Classifier classifier = AbstractClassifier.makeCopy(scheme.template());
            classifier.buildClassifier(training);
            Evaluation evaluation = new Evaluation(training);
            evaluation.evaluateModel(classifier, test);
            return new SchemeEvaluation(scheme, training.numInstances(), test.numInstances(),
                    usedAttributes(classifier, training), PredictionMetrics.of(evaluation, positiveClass(training)));
        } catch (Exception e) {
            // Le API di Weka dichiarano Exception generiche: si conserva la causa e si aggiunge il contesto.
            throw new IllegalStateException("Valutazione non riuscita per " + scheme.name(), e);
        }
    }

    private static int positiveClass(Instances training) {
        int positive = training.classAttribute().indexOfValue(PredictionDataset.POSITIVE_CLASS);
        if (positive < 0) {
            throw new IllegalArgumentException("Classe " + PredictionDataset.POSITIVE_CLASS + " assente in "
                    + training.relationName());
        }
        return positive;
    }

    private static int usedAttributes(Classifier classifier, Instances training) {
        if (classifier instanceof AttributeSelectedClassifier selected) {
            return (int) selected.measureNumAttributesSelected();
        }
        if (classifier instanceof FilteredClassifier filtered) {
            return filtered.getFilter().getOutputFormat().numAttributes() - 1;
        }
        return training.numAttributes() - 1;
    }
}
