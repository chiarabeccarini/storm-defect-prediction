package it.uniroma2.isw2.storm.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GitRepository {

    private static final int GIT_NOT_FOUND = 1;
    private static final String REV_PARSE = "rev-parse";

    private final Path workTree;

    public GitRepository(Path workTree) {
        if (!Files.isDirectory(workTree.resolve(".git"))) {
            throw new IllegalArgumentException("Non è un repository Git: " + workTree);
        }
        this.workTree = workTree;
    }

    /** Commit puntato dal tag, indicato sempre come {@code refs/tags/...} perché alcuni nomi sono anche branch. */
    public Optional<String> commitOfTag(String tagName) throws IOException {
        requireNotBlank(tagName, "Nome del tag mancante");
        List<String> command = gitCommand(REV_PARSE, "--verify", "--quiet", "refs/tags/" + tagName + "^{commit}");
        GitResult revParse = execute(command);
        if (revParse.exitCode() == GIT_NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(checked(command, revParse).trim());
    }

    public String currentBranch() throws IOException {
        return run(REV_PARSE, "--abbrev-ref", "HEAD").trim();
    }

    /**
     * Porta la copia di lavoro sul riferimento indicato e verifica che il commit attivo sia
     * davvero quello atteso: un checkout non riuscito non deve far misurare la release sbagliata.
     */
    public void checkout(String reference) throws IOException {
        requireNotBlank(reference, "Riferimento per il checkout mancante");
        String expectedCommit = run(REV_PARSE, "--verify", reference + "^{commit}").trim();
        run("checkout", "--quiet", reference);
        String activeCommit = run(REV_PARSE, "HEAD").trim();
        if (!activeCommit.equals(expectedCommit)) {
            throw new IOException("Checkout di " + reference + " non riuscito: attivo " + activeCommit);
        }
    }

    public List<String> trackedFiles() throws IOException {
        return run("-c", "core.quotepath=off", "ls-files").lines().toList();
    }

    private String run(String... arguments) throws IOException {
        List<String> command = gitCommand(arguments);
        return checked(command, execute(command));
    }

    private List<String> gitCommand(String... arguments) {
        List<String> command = new ArrayList<>(List.of("git", "-C", workTree.toString()));
        command.addAll(List.of(arguments));
        return command;
    }

    private static String checked(List<String> command, GitResult result) throws IOException {
        if (result.exitCode() != 0) {
            throw new IOException("Comando git fallito (" + String.join(" ", command) + "): " + result.output());
        }
        return result.output();
    }

    private static GitResult execute(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return new GitResult(process.waitFor(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Comando git interrotto: " + String.join(" ", command), e);
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private record GitResult(int exitCode, String output) {
    }
}
