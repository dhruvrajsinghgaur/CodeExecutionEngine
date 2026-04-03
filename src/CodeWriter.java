import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {
    public static void write(String code, String className){
        try{

            File dir = new File("temp");
            if(!dir.exists()){
                dir.mkdir();
            }

            FileWriter writer = new FileWriter("temp/" +  className + ".java");

            writer.write(code);

            writer.close();

        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
