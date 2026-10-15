package it.uniroma2.isw2.storm.metrics;

import java.util.Arrays;
import java.util.List;

/**
 * Sottosistema di un file: la parte del percorso che precede la cartella {@code src} o {@code test}, cioè il
 * modulo che lo contiene (es. {@code external/storm-kafka}, {@code storm-core}). Il codice che prima della
 * suddivisione in moduli stava direttamente in {@code src} e {@code test} appartiene alla radice.
 */
final class Subsystems {

    static final String ROOT = "(radice)";

    private static final List<String> SOURCE_FOLDERS = List.of("src", "test");

    private Subsystems() {
    }

    static String of(String path) {
        String[] folders = path.split("/");
        for (int depth = 0; depth < folders.length - 1; depth++) {
            if (SOURCE_FOLDERS.contains(folders[depth])) {
                return depth == 0 ? ROOT : String.join("/", Arrays.copyOfRange(folders, 0, depth));
            }
        }
        throw new IllegalArgumentException("Percorso senza cartella src o test: " + path);
    }
}
