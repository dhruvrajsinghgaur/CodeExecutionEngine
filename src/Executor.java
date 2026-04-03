import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

public class Executor {
    public static void run(String className){
        try{
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", "temp", className
            );

            Process process = pb.start();

            // Thread 1 -> Read Program Output
            Thread outputThread = new Thread(()->{
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            // Thread 2 -> Read Error
            Thread errorThread = new Thread(()->{
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream())
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Error : " + line);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            // Thread 3 -> TAKE USER INPUT AND SEND TO PROCESS
            Thread inputThread = new Thread(()->{
                try {
                    BufferedReader userInput = new BufferedReader(
                            new InputStreamReader(System.in)
                    );

                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(process.getOutputStream())
                    );

                    String line;

                    while ((line = userInput.readLine()) != null) {
                        try {
                            if (!process.isAlive()) break;
                            writer.write(line);
                            writer.newLine();
                            writer.flush();
                        }catch (Exception e){
                            break;
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();
            inputThread.start();

            boolean finished = process.waitFor(5,TimeUnit.SECONDS);

            if(!finished){
                process.destroy();
                System.out.println("Execution TimeOut");
            }

            inputThread.interrupt();

            // wait for threads to finish.
            outputThread.join();
            errorThread.join();

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
