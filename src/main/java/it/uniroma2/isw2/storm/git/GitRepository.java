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

    private final Path workTree;

    public GitRepository(Path workTree) {
        if (!Files.isDirectory(workTree.resolve(".git"))) {
            throw new IllegalArgumentException("Non è un repository Git: " + workTree);
        }
        this.workTree = workTree;
    }

    /** Commit puntato dal tag, indicato sempre come {@code refs/tags/...} perché alcuni nomi sono anche branch. */
    public Optional<String> commitOfTag(String tagName) throws IOException {
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("Nome del tag mancante");
        }
        GitResult revParse = run("rev-parse", "--verify", "--quiet", "refs/tags/" + tagName + "^{commit}");
        if (revParse.exitCode() == GIT_NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(revParse.output().trim());
    }

    private GitResult run(String... arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", workTree.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = waitFor(process, command);
        if (exitCode != 0 && exitCode != GIT_NOT_FOUND) {
            throw new IOException("Comando git fallito (" + String.join(" ", command) + "): " + output);
        }
        return new GitResult(exitCode, output);
    }

    private static int waitFor(Process process, List<String> command) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Comando git interrotto: " + String.join(" ", command), e);
        }
    }

    private record GitResult(int exitCode, String output) {
    }
}
