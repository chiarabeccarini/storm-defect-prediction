package it.uniroma2.isw2.storm.prediction;

/** Valutazione di uno schema su un fold di una ripetizione della cross-validation; ripetizioni e fold partono da 1. */
public record FoldResult(int run, int fold, SchemeEvaluation evaluation) {

    public FoldResult {
        if (run < 1 || fold < 1 || evaluation == null) {
            throw new IllegalArgumentException("Risultato di fold non valido: ripetizione " + run + ", fold " + fold);
        }
    }
}
