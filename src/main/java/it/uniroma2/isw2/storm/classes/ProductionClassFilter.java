package it.uniroma2.isw2.storm.classes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Perimetro delle classi del dataset: solo file .java del prodotto. Sono escluse le classi
 * di test, il codice generato da Thrift, gli esempi e gli strumenti di build. Le cartelle si
 * confrontano per nome esatto, così il package di produzione {@code testing} resta incluso.
 */
public final class ProductionClassFilter {

    private static final Set<String> EXCLUDED_FOLDERS = Set.of("test", "tests", "generated", "examples");
    private static final Set<String> EXCLUDED_MODULES = Set.of("storm-buildtools", "dev-tools");

    private ProductionClassFilter() {
    }

    public static boolean isProductionClass(String path) {
        if (path == null || !path.endsWith(".java")) {
            return false;
        }
        String[] pathParts = path.split("/");
        List<String> folders = Arrays.asList(pathParts).subList(0, pathParts.length - 1);
        if (!folders.isEmpty() && EXCLUDED_MODULES.contains(folders.get(0))) {
            return false;
        }
        return folders.stream().noneMatch(folder -> EXCLUDED_FOLDERS.contains(folder) || folder.endsWith("-examples"));
    }
}
