import java.io.File;

public class Cleaner {
    public static void clean(String className){
        new File("temp/" + className + ".java").delete();
        new File("temp/" + className + ".class").delete();
    }
}
