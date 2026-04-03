import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.println("Enter class name (must match in code): ");

        String className = input.nextLine();

        String code = InputManager.readCode();

        CodeWriter.write(code, className);

        boolean compiler = Compiler.compile(className);

        if (compiler) {
            System.out.println("Compilation successful");
            Executor.run(className);
        }else {
            System.out.println("Compilation Error");
        }

        Cleaner.clean(className);
    }
}
