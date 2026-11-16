package it.uniroma2.isw2.storm.prediction;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * I quattro dataset dell'analisi What-If, tutti con gli attributi di A: A è il dataset completo, B+ le sue righe con
 * almeno uno smell, C quelle senza smell, B una copia di B+ in cui cambia solo il numero di smell, portato a 0.
 */
public record WhatIfDatasets(Instances full, Instances withSmells, Instances smellsRemoved, Instances withoutSmells) {

    public WhatIfDatasets {
        if (full == null || withSmells == null || smellsRemoved == null || withoutSmells == null) {
            throw new IllegalArgumentException("Dataset dell'analisi What-If incompleti");
        }
    }

    // Con log10(x + 1) lo zero resta zero: NSmells > 0 e NSmells = 0 separano le stesse righe dei valori grezzi.
    public static WhatIfDatasets derive(Instances full, String smellsAttribute, String relationPrefix) {
        int smells = smellsIndex(full, smellsAttribute);
        Instances withSmells = emptyCopy(full, relationPrefix + "Bplus");
        Instances smellsRemoved = emptyCopy(full, relationPrefix + "B");
        Instances withoutSmells = emptyCopy(full, relationPrefix + "C");
        for (Instance row : full) {
            if (row.isMissing(smells)) {
                throw new IllegalArgumentException("Valore di " + smellsAttribute + " mancante in " + full.relationName());
            }
            if (row.value(smells) > 0) {
                withSmells.add(row);
                smellsRemoved.add(row);
                smellsRemoved.lastInstance().setValue(smells, 0);
            } else {
                withoutSmells.add(row);
            }
        }
        return new WhatIfDatasets(full, withSmells, smellsRemoved, withoutSmells);
    }

    static int smellsIndex(Instances dataset, String smellsAttribute) {
        Attribute attribute = dataset.attribute(smellsAttribute);
        if (attribute == null || !attribute.isNumeric() || attribute.index() == dataset.classIndex()) {
            throw new IllegalArgumentException("Metrica numerica " + smellsAttribute + " assente in "
                    + dataset.relationName());
        }
        return attribute.index();
    }

    // Instances.add copia la riga, quindi modificare B non cambia B+ né A.
    private static Instances emptyCopy(Instances dataset, String relationName) {
        Instances copy = new Instances(dataset, dataset.numInstances());
        copy.setRelationName(relationName);
        return copy;
    }
}
