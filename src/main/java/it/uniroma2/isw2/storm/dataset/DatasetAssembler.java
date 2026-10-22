package it.uniroma2.isw2.storm.dataset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Unisce i CSV dei passi precedenti nel dataset finale, con le colonne nell'ordine richiesto: progetto, classe,
 * Release ID, metriche, NSmells e Bugginess. I valori sono copiati come testo, così restano identici a quelli
 * calcolati; ogni CSV deve contenere le stesse coppie classe-release nello stesso ordine.
 */
public final class DatasetAssembler {

    private static final Logger LOGGER = Logger.getLogger(DatasetAssembler.class.getName());
    private static final String SEPARATOR = ",";
    private static final List<String> KEY_COLUMNS = List.of("Release ID", "Class");
    private static final List<String> DATASET_KEY_COLUMNS = List.of("Project Name", "Class Name", "Release ID");
    private static final String SMELLS_COLUMN = "NSmells";
    private static final String BUGGINESS_COLUMN = "Bugginess";
    private static final String BUGGY = "Yes";

    private DatasetAssembler() {
    }

    /** I CSV vanno indicati nell'ordine in cui le loro colonne compaiono nel dataset. */
    public static void assemble(String projectName, List<Path> stepCsvFiles, Path datasetCsv) throws IOException {
        if (projectName == null || projectName.isBlank() || stepCsvFiles.isEmpty()) {
            throw new IllegalArgumentException("Nome del progetto o CSV da unire mancanti");
        }
        List<List<String[]>> tables = new ArrayList<>();
        for (Path csvFile : stepCsvFiles) {
            tables.add(readCsv(csvFile));
        }
        requireUniqueKeys(tables.get(0), stepCsvFiles.get(0));
        for (int table = 1; table < tables.size(); table++) {
            requireSameKeys(tables.get(0), tables.get(table), stepCsvFiles.get(table));
        }
        List<String[]> dataset = joinColumns(projectName, tables);
        logSummary(dataset);
        List<String> lines = dataset.stream().map(row -> String.join(SEPARATOR, row)).toList();
        Files.createDirectories(datasetCsv.toAbsolutePath().getParent());
        Files.writeString(datasetCsv, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static List<String[]> readCsv(Path csvFile) throws IOException {
        List<String[]> rows = Files.readAllLines(csvFile, StandardCharsets.UTF_8).stream()
                .map(line -> line.split(SEPARATOR, -1))
                .toList();
        if (rows.size() < 2 || !Arrays.asList(rows.get(0)).subList(0, KEY_COLUMNS.size()).equals(KEY_COLUMNS)) {
            throw new IllegalArgumentException("Intestazione inattesa o nessuna riga in " + csvFile);
        }
        int columns = rows.get(0).length;
        if (rows.stream().anyMatch(row -> row.length != columns)) {
            throw new IllegalArgumentException("Righe con un numero di colonne diverso dall'intestazione in " + csvFile);
        }
        return rows;
    }

    private static void requireUniqueKeys(List<String[]> table, Path csvFile) {
        Set<String> keys = new HashSet<>();
        for (String[] row : table.subList(1, table.size())) {
            if (!keys.add(row[0] + SEPARATOR + row[1])) {
                throw new IllegalStateException("Coppia classe-release duplicata in " + csvFile + ": " + row[1]);
            }
        }
    }

    private static void requireSameKeys(List<String[]> reference, List<String[]> table, Path csvFile) {
        if (table.size() != reference.size()) {
            throw new IllegalStateException(csvFile + " ha " + (table.size() - 1) + " righe invece di " + (reference.size() - 1));
        }
        for (int row = 1; row < table.size(); row++) {
            if (!table.get(row)[0].equals(reference.get(row)[0]) || !table.get(row)[1].equals(reference.get(row)[1])) {
                throw new IllegalStateException("Coppie classe-release non allineate in " + csvFile + " alla riga " + (row + 1));
            }
        }
    }

    private static List<String[]> joinColumns(String projectName, List<List<String[]>> tables) {
        List<String[]> dataset = new ArrayList<>();
        for (int row = 0; row < tables.get(0).size(); row++) {
            List<String> fields = new ArrayList<>();
            String[] key = tables.get(0).get(row);
            fields.addAll(row == 0 ? DATASET_KEY_COLUMNS : List.of(projectName, key[1], key[0]));
            for (List<String[]> table : tables) {
                String[] values = table.get(row);
                fields.addAll(Arrays.asList(values).subList(KEY_COLUMNS.size(), values.length));
            }
            dataset.add(fields.toArray(String[]::new));
        }
        return dataset;
    }

    // Per i risultati: quante classi ha ogni release e quante sono difettose, e la distribuzione degli smell.
    private static void logSummary(List<String[]> dataset) {
        List<String> header = Arrays.asList(dataset.get(0));
        int releaseColumn = header.indexOf(DATASET_KEY_COLUMNS.get(2));
        int smellsColumn = header.indexOf(SMELLS_COLUMN);
        int bugginessColumn = header.indexOf(BUGGINESS_COLUMN);
        if (smellsColumn < 0 || bugginessColumn < 0) {
            throw new IllegalStateException("Colonne " + SMELLS_COLUMN + " o " + BUGGINESS_COLUMN + " mancanti nel dataset");
        }
        List<String[]> rows = dataset.subList(1, dataset.size());
        Map<String, List<String>> labelsByRelease = rows.stream().collect(Collectors.groupingBy(row -> row[releaseColumn],
                LinkedHashMap::new, Collectors.mapping(row -> row[bugginessColumn], Collectors.toList())));
        labelsByRelease.forEach((release, labels) -> {
            long buggy = labels.stream().filter(BUGGY::equals).count();
            LOGGER.info(() -> String.format("Release %s: %d classi, %d difettose (%.1f%%)", release, labels.size(), buggy,
                    100.0 * buggy / labels.size()));
        });
        IntSummaryStatistics smells = rows.stream().mapToInt(row -> Integer.parseInt(row[smellsColumn])).summaryStatistics();
        LOGGER.info(() -> String.format("Dataset: %d righe, %d colonne; NSmells da %d a %d, media %.2f", rows.size(),
                header.size(), smells.getMin(), smells.getMax(), smells.getAverage()));
    }
}
