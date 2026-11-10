package it.uniroma2.isw2.storm.prediction;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SpreadSubsample;

import java.util.Arrays;
import java.util.List;

/**
 * Le quattro configurazioni di ogni classificatore. Feature selection e balancing stanno dentro i meta-classificatori,
 * così Weka li applica solo al training di ogni valutazione e il test resta intatto; quando ci sono entrambi, la
 * feature selection viene prima del balancing.
 */
public enum Configuration {
    BASE("base"),
    FEATURE_SELECTION("FS"),
    BALANCING("balancing"),
    FEATURE_SELECTION_AND_BALANCING("FS + balancing");

    private final String label;

    Configuration(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Classifier wrap(Classifier classifier) {
        return switch (this) {
            case BASE -> classifier;
            case FEATURE_SELECTION -> attributeSelected(classifier);
            case BALANCING -> filtered(classifier, undersampling());
            case FEATURE_SELECTION_AND_BALANCING -> filtered(classifier, inSequence(attributeSelection(), undersampling()));
        };
    }

    /** Metriche che la feature selection sceglie su un training: le stesse usate dalle configurazioni con FS. */
    public static List<String> selectedMetrics(Instances training) {
        weka.attributeSelection.AttributeSelection selection = new weka.attributeSelection.AttributeSelection();
        selection.setEvaluator(new CfsSubsetEval());
        selection.setSearch(new BestFirst());
        try {
            selection.SelectAttributes(training);
            return Arrays.stream(selection.selectedAttributes())
                    .filter(index -> index != training.classIndex())
                    .mapToObj(index -> training.attribute(index).name())
                    .toList();
        } catch (Exception e) {
            // SelectAttributes dichiara Exception generiche: si conserva la causa e si aggiunge il contesto.
            throw new IllegalStateException("Feature selection non riuscita su " + training.relationName(), e);
        }
    }

    // CfsSubsetEval e BestFirst con le opzioni di default: ricerca forward.
    private static AttributeSelectedClassifier attributeSelected(Classifier classifier) {
        AttributeSelectedClassifier selected = new AttributeSelectedClassifier();
        selected.setEvaluator(new CfsSubsetEval());
        selected.setSearch(new BestFirst());
        selected.setClassifier(classifier);
        return selected;
    }

    private static AttributeSelection attributeSelection() {
        AttributeSelection selection = new AttributeSelection();
        selection.setEvaluator(new CfsSubsetEval());
        selection.setSearch(new BestFirst());
        return selection;
    }

    // Spread 1.0: la classe maggioritaria viene ridotta fino ad avere tante istanze quante la minoritaria.
    private static SpreadSubsample undersampling() {
        SpreadSubsample undersampling = new SpreadSubsample();
        undersampling.setDistributionSpread(1.0);
        return undersampling;
    }

    private static FilteredClassifier filtered(Classifier classifier, Filter filter) {
        FilteredClassifier filtered = new FilteredClassifier();
        filtered.setFilter(filter);
        filtered.setClassifier(classifier);
        return filtered;
    }

    private static MultiFilter inSequence(Filter... filters) {
        MultiFilter sequence = new MultiFilter();
        sequence.setFilters(filters);
        return sequence;
    }
}
