# Code Execution Engine (JavaFX IDE)

A mini Java IDE built using JavaFX that allows:
- Writing Java code
- Compiling dynamically
- Executing with real-time input/output
- Interactive terminal (like VS Code)

## 🚀 Features
- Live code execution
- Terminal-style input/output
- Multithreaded process handling
- Custom Java compiler + executor

## 🛠 Tech Stack
- Java
- JavaFX
- ProcessBuilder
- Multithreading
- Maven (project build & dependency management)

## ▶️ How to Run

1. Clone the repository
   git clone https://github.com/username/CodeExecutionEngine.git
   cd CodeExecutionEngine

2. Install dependencies & run with Maven
   mvn clean javafx:run

3. If using an IDE (IntelliJ/Eclipse):
    - Import the project as a Maven project.
    - Make sure JavaFX SDK is set up in your IDE.
    - Add VM options if needed:
      --module-path "PATH_TO_JAVAFX/lib" --add-modules javafx.controls,javafx.fxml

## 📌 Future Improvements
- Syntax highlighting (RichTextFX)
- File system support (open/save files)
- Multiple tabs for editing
- Debugger & error highlighting
- Better terminal integration