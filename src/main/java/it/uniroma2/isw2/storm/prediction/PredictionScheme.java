package it.uniroma2.isw2.storm.prediction;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.RandomForest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Un classificatore in una configurazione: è l'unità confrontata dalla validazione. */
public record PredictionScheme(String classifier, Configuration configuration, Classifier template) {

    public static final String BASELINE = "ZeroR";

    public PredictionScheme {
        if (classifier == null || classifier.isBlank() || configuration == null || template == null) {
            throw new IllegalArgumentException("Schema incompleto: " + classifier + " " + configuration);
        }
    }

    public String name() {
        return classifier + " " + configuration.label();
    }

    /** ZeroR come baseline, poi Random Forest, Naive Bayes e IBk (k = 1) con i parametri di default di Weka. */
    public static List<PredictionScheme> all() {
        List<PredictionScheme> schemes = new ArrayList<>();
        schemes.add(new PredictionScheme(BASELINE, Configuration.BASE, new ZeroR()));
        List<Supplier<Classifier>> classifiers = List.of(RandomForest::new, NaiveBayes::new, IBk::new);
        for (Supplier<Classifier> classifier : classifiers) {
            for (Configuration configuration : Configuration.values()) {
                Classifier base = classifier.get();
                schemes.add(new PredictionScheme(base.getClass().getSimpleName(), configuration, configuration.wrap(base)));
            }
        }
        return List.copyOf(schemes);
    }
}
