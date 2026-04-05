import java.io.*;
import java.util.function.Consumer;
import java.util.regex.*;

public class Compiler {

    public static void compile(String code, Consumer<String> onSuccess, Consumer<String> onError) {
        try {
            File dir = new File("temp");
            if (!dir.exists()) dir.mkdir();

            // Clean up old files
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".java") || f.getName().endsWith(".class")) {
                    f.delete();
                }
            }

            String className = extractClassName(code);
            if (className == null) {
                onError.accept("No public class found in code.");
                return;
            }

            File file = new File(dir, className + ".java");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(code);
            }

            ProcessBuilder pb = new ProcessBuilder("javac", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errors = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }

            process.waitFor();

            if (errors.length() > 0) {
                onError.accept(errors.toString());
            } else {
                onSuccess.accept(className);
            }

        } catch (Exception e) {
            onError.accept("Compilation failed: " + e.getMessage());
        }
    }

    public static String extractClassName(String code) {
        Pattern pattern = Pattern.compile("public\\s+(?:class|interface|enum)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}