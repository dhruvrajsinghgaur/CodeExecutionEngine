import java.io.*;
import java.util.function.Consumer;

public class Compiler {

    public static void compile(String code, Consumer<String> onSuccess, Consumer<String> onError) {
        try {
            File dir = new File("temp");
            if (!dir.exists()) dir.mkdir();

            // Assume class name = Main
            String className = "Main";
            File file = new File(dir, className + ".java");

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(code);
            writer.close();

            ProcessBuilder pb = new ProcessBuilder(
                    "javac", file.getAbsolutePath()
            );

            Process process = pb.start();

            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
            );

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
}
