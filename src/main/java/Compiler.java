import java.io.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {

    /**
     * Compiles the given source code into the provided tempDir.
     * This method is blocking — always call it from a background thread.
     *
     * @param code      Java source code to compile
     * @param tempDir   Per-tab temp directory (avoids collisions between tabs)
     * @param onSuccess Called with the compiled class name on success
     * @param onError   Called with the compiler error output on failure
     */
    public static void compile(String code,
                               File tempDir,
                               Consumer<String> onSuccess,
                               Consumer<String> onError) {
        Process process = null;
        try {
            // Ensure temp dir exists and is clean
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            File[] existing = tempDir.listFiles();
            if (existing != null) {
                for (File f : existing) {
                    if (f.getName().endsWith(".java") || f.getName().endsWith(".class")) {
                        f.delete();
                    }
                }
            }

            String className = extractClassName(code);
            if (className == null) {
                onError.accept("No public class found in code.");
                return;
            }

            File sourceFile = new File(tempDir, className + ".java");
            try (BufferedWriter w = new BufferedWriter(new FileWriter(sourceFile))) {
                w.write(code);
            }

            ProcessBuilder pb = new ProcessBuilder("javac", sourceFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            process = pb.start();

            // Read compiler output (errors go here on failure)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                onSuccess.accept(className);
            } else {
                onError.accept(output.toString());
            }

        } catch (InterruptedException e) {
            // Thread was interrupted (user clicked Stop during compilation)
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            onError.accept("Compilation cancelled.\n");
        } catch (Exception e) {
            onError.accept("Compilation failed: " + e.getMessage() + "\n");
        }
    }

    /**
     * Extracts the name of the first public class, interface, record, or enum.
     */
    public static String extractClassName(String code) {
        Pattern pattern = Pattern.compile(
                "public\\s+(?:class|interface|enum|record)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }
}