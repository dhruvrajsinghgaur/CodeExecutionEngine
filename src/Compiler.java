import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Compiler {
    public static boolean compile(String className) {
        try{
            ProcessBuilder pb = new ProcessBuilder(
                    "javac", "temp/" +  className + ".java"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while((line = outputReader.readLine()) != null){
                System.out.println(line);
            }

            int exitCode = process.waitFor();

            return exitCode == 0;

        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
