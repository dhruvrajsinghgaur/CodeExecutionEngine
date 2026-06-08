import javafx.application.Platform;

import java.io.*;
import java.util.function.Consumer;

/**
 * Runs a compiled Java class in a child process and streams its I/O.
 * Each EditorTab owns its own Executor instance — no shared static state.
 */
public class Executor {

    private Process process;
    private BufferedWriter stdinWriter;

    /**
     * Launches the compiled class and streams its output back via outputHandler.
     * Blocks the calling thread until the process exits and all output is consumed.
     * Always call from a background thread.
     *
     * @param className     Name of the class to run (e.g. "Main")
     * @param tempDir       Directory containing the .class file
     * @param outputHandler Receives output characters; called on the JavaFX thread
     */
    public void run(String className, File tempDir, Consumer<String> outputHandler) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", tempDir.getAbsolutePath(), className);
            pb.redirectErrorStream(true); // stderr → stdout

            process = pb.start();
            stdinWriter = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));

            // Daemon I/O thread — reads output char-by-char and dispatches to FX thread
            Thread ioThread = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    int ch;
                    while ((ch = in.read()) != -1) {
                        final char c = (char) ch;
                        Platform.runLater(() -> outputHandler.accept(String.valueOf(c)));
                    }
                } catch (IOException ignored) {
                    // Process ended normally or was force-killed — both are fine here
                }
            });
            ioThread.setDaemon(true);
            ioThread.start();

            process.waitFor();  // Block until process exits
            ioThread.join();    // Wait for all remaining output to be dispatched

        } catch (InterruptedException e) {
            // Calling thread was interrupted (Stop button) — kill child process
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            Platform.runLater(() -> outputHandler.accept("\n[Process interrupted]\n"));
        } catch (Exception e) {
            Platform.runLater(() ->
                    outputHandler.accept("\nExecution failed: " + e.getMessage() + "\n"));
        }
    }

    /**
     * Sends a line of text to the running process's stdin.
     */
    public void sendInput(String input) {
        if (!isRunning()) return;
        try {
            stdinWriter.write(input);
            stdinWriter.newLine();
            stdinWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Forcibly kills the running process. Safe to call at any time.
     */
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    /**
     * Returns true if a process is currently running.
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }
}