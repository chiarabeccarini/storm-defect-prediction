package it.uniroma2.isw2.storm.prediction;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.experiment.Stats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Scrittura e lettura dei file ARFF con i convertitori di Weka; la classe è sempre l'ultimo attributo. */
public final class ArffFiles {

    private ArffFiles() {
    }

    // Instances.toString() è il formato ARFF di Weka. ArffSaver darebbe lo stesso contenuto, ma con fine riga misti
    // e caricando all'avvio librerie native di algebra lineare che stampano avvisi estranei al passo.
    public static void write(Instances instances, Path arffFile) throws IOException {
        Files.createDirectories(arffFile.toAbsolutePath().getParent());
        Files.writeString(arffFile, instances + "\n", StandardCharsets.UTF_8);
    }

    public static Instances read(Path arffFile) throws IOException {
        ArffLoader loader = new ArffLoader();
        loader.setFile(arffFile.toFile());
        Instances instances = loader.getDataSet();
        instances.setClassIndex(instances.numAttributes() - 1);
        return instances;
    }

    /** Per i controlli nei log: istanze per valore della classe, numero di attributi e valori della classe in ordine. */
    public static String describe(Instances instances) {
        Attribute classAttribute = instances.classAttribute();
        int[] counts = instances.attributeStats(instances.classIndex()).nominalCounts;
        String classCounts = IntStream.range(0, classAttribute.numValues())
                .mapToObj(value -> String.format("%d %s (%.1f%%)", counts[value], classAttribute.value(value),
                        100.0 * counts[value] / instances.numInstances()))
                .collect(Collectors.joining(", "));
        String classValues = IntStream.range(0, classAttribute.numValues())
                .mapToObj(classAttribute::value)
                .collect(Collectors.joining(",", "{", "}"));
        return String.format("%d istanze, %s; %d attributi, classe %s %s", instances.numInstances(), classCounts,
                instances.numAttributes(), classAttribute.name(), classValues);
    }

    public static String numericRange(Instances instances, String attributeName) {
        Attribute attribute = instances.attribute(attributeName);
        if (attribute == null || !attribute.isNumeric()) {
            throw new IllegalArgumentException("Attributo numerico " + attributeName + " assente in " + instances.relationName());
        }
        Stats stats = instances.attributeStats(attribute.index()).numericStats;
        return String.format("%s da %.2f a %.2f", attributeName, stats.min, stats.max);
    }
}
