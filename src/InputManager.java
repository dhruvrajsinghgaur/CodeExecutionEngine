import java.util.Scanner;

public class InputManager {
    public static String readCode(){
        Scanner sc = new Scanner(System.in);

        StringBuilder code = new StringBuilder();

        System.out.println("Enter java code: ");
        System.out.println("Type 'END' in the last line to end the code");

        while(true){
            String line = sc.nextLine();
            if (line.equals("END")) break;
            code.append(line).append("\n");
        }

        return code.toString();
    }
}
