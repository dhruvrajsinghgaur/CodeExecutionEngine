import java.io.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {

    public static void compile(String code,
                               File tempDir,
                               Consumer<String> onSuccess,
                               Consumer<String> onError) {
        Process process = null;
        try {
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
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            onError.accept("Compilation cancelled.\n");
        } catch (Exception e) {
            onError.accept("Compilation failed: " + e.getMessage() + "\n");
        }
    }

    public static String extractClassName(String code) {
        Pattern pattern = Pattern.compile(
                "public\\s+(?:class|interface|enum|record)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }
}