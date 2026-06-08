import javafx.application.Platform;

import java.io.*;
import java.util.function.Consumer;

public class Executor {

    private Process process;
    private BufferedWriter stdinWriter;

    public void run(String className, File tempDir, Consumer<String> outputHandler) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", tempDir.getAbsolutePath(), className);
            pb.redirectErrorStream(true); // stderr → stdout

            process = pb.start();
            stdinWriter = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));

            Thread ioThread = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    int ch;
                    while ((ch = in.read()) != -1) {
                        final char c = (char) ch;
                        Platform.runLater(() -> outputHandler.accept(String.valueOf(c)));
                    }
                } catch (IOException ignored) {

                }
            });
            ioThread.setDaemon(true);
            ioThread.start();

            process.waitFor();
            ioThread.join();

        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            Platform.runLater(() -> outputHandler.accept("\n[Process interrupted]\n"));
        } catch (Exception e) {
            Platform.runLater(() ->
                    outputHandler.accept("\nExecution failed: " + e.getMessage() + "\n"));
        }
    }

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

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }
}