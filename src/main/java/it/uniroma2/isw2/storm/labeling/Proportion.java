package it.uniroma2.isw2.storm.labeling;

import java.util.List;

/**
 * Proportion Total: P è la media di (FV - IV) / (FV - OV) sui ticket con Injected Version nota e FV successiva alla OV.
 * La mediana serve solo a descrivere la distribuzione nel report.
 */
record Proportion(double mean, double median, int tickets) {

    Proportion {
        if (tickets < 1 || mean < 1) {
            throw new IllegalArgumentException("Proportion non valida: P = " + mean + " su " + tickets + " ticket");
        }
    }

    static Proportion of(List<TicketLifecycle> knownLifecycles) {
        List<Double> ratios = knownLifecycles.stream()
                .filter(lifecycle -> lifecycle.fixedVersion() > lifecycle.openingVersion())
                .map(Proportion::ratio)
                .sorted()
                .toList();
        if (ratios.isEmpty()) {
            throw new IllegalArgumentException("Nessun ticket con cui calcolare P");
        }
        double mean = ratios.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return new Proportion(mean, median(ratios), ratios.size());
    }

    /** IV = FV - (FV - OV) * P, arrotondata all'intero più vicino e mai prima della release 1. */
    int estimateInjectedVersion(int openingVersion, int fixedVersion) {
        // Con FV non successiva alla OV la formula non si applica: IV = OV lascia vuoto l'intervallo [IV, FV).
        if (fixedVersion <= openingVersion) {
            return openingVersion;
        }
        return (int) Math.max(1, unboundedEstimate(openingVersion, fixedVersion));
    }

    /** Stima prima del limite alla release 1: serve a contare i ticket per cui il limite interviene. */
    long unboundedEstimate(int openingVersion, int fixedVersion) {
        return Math.round(fixedVersion - (fixedVersion - openingVersion) * mean);
    }

    private static double ratio(TicketLifecycle lifecycle) {
        return (double) (lifecycle.fixedVersion() - lifecycle.injectedVersion())
                / (lifecycle.fixedVersion() - lifecycle.openingVersion());
    }

    private static double median(List<Double> sortedValues) {
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return sortedValues.get(middle);
        }
        return (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2;
    }
}
