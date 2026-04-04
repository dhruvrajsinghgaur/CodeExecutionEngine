import javafx.application.Platform;
import java.io.*;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

public class Executor {

    public static Process process;
    public static BufferedWriter writer;

    public static void run(String className, Consumer<String> outputHandler) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", "temp", className
            );

            process = pb.start();

            writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream())
            );

            Thread outputThread = new Thread(() -> {
                try {
                    InputStream is = process.getInputStream();
                    int ch;

                    while ((ch = is.read()) != -1) {
                        char c = (char) ch;

                        Platform.runLater(() -> outputHandler.accept(String.valueOf(c)));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Thread errorThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream())
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String finalLine = "Error: " + line;
                        Platform.runLater(() -> outputHandler.accept(finalLine));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            process.waitFor();

            outputThread.join();
            errorThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendInput(String input) {
        try {
            if (process != null && process.isAlive()) {
                writer.write(input);
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

