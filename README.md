# CodeExecutionEngine

A desktop Java IDE built with JavaFX that compiles and runs Java programs without leaving the
application — edit, run, and interact with your program's stdin/stdout in one window.

> **Add a screenshot here** — run the app, take a screenshot, save it as `docs/screenshot.png`,
> and replace this line with:
> `![CodeExecutionEngine screenshot](docs/screenshot.png)`

---

## What it does

You write Java code in an editor tab, click **Run**, and the engine:

1. Extracts the public class name from your source using a regex (`class`, `interface`, `enum`,
   and `record` all supported)
2. Writes the source to an isolated temp directory and invokes `javac` as a subprocess
3. On success, spawns a `java` subprocess and streams its output character-by-character into
   the output console
4. Lets you type into the **input field** and send text to the program's stdin while it runs
5. Lets you click **Stop** to forcibly kill the process at any time

Each tab is completely independent — its own temp directory, its own process, its own output.

---

## Features

- **Multi-tab editor** — each tab has isolated state, a UUID-named temp directory, and its own
  `Executor` instance; two tabs can both declare a class named `Main` and run simultaneously
- **Syntax highlighting** — keywords, strings, chars, comments, annotations, and numbers
  highlighted via RichTextFX, debounced at 150 ms so it does not fire on every keystroke
- **Live stdin** — type input into the field at the bottom of each tab and send it to the
  running process; supports programs that call `Scanner.nextLine()` interactively
- **Real-time output** — output is read character-by-character, not line-by-line, so prompts
  like `Enter your name:` appear immediately without waiting for a newline
- **Open / Save / Save As** — file chooser integration; tab title shows `*` when there are
  unsaved changes
- **Unsaved changes guard** — closing a modified tab prompts to save, cancel, or discard
- **Compile errors rendered inline** — `javac` stderr is merged into the output stream and
  displayed in the console with a compile-error header

---

## Architecture

### Class overview

| Class | Role |
|---|---|
| `IDE` | JavaFX Application entry point. Builds the toolbar and TabPane, wires all button actions. |
| `EditorTab` | Extends Tab. Contains the CodeArea, output TextArea, and stdin TextField. Owns one Executor and one UUID temp directory. |
| `Compiler` | Static utility. Extracts the class name, writes the .java file, runs javac via ProcessBuilder, and calls back with success or failure. |
| `Executor` | One instance per tab. Runs java via ProcessBuilder, drains output on a daemon IO thread, exposes sendInput() for stdin, and stop() for forcible kill. |
| `SyntaxHighlighter` | Stateless. Takes a String of Java source and returns StyleSpans for RichTextFX based on regex-matched token types. |

### Threading model

```
JavaFX UI Thread
│
│  user clicks Run
│
└──► EditorTab.runCode()
         │
         │  new daemon Thread (runThread)
         │
         ├──► Compiler.compile()     (blocks: writes .java file, waits for javac)
         │         │
         │         │  onSuccess callback
         │         │
         │         └──► Executor.run()    (blocks: waits for java process to exit)
         │                   │
         │                   └──► daemon IO Thread
         │                             reads process stdout byte by byte
         │                             Platform.runLater to append to output console
         │
         └── Platform.runLater to re-enable Run button and update status label
```

The UI thread never blocks. All output console and status label updates are dispatched
through `Platform.runLater`.

### Process isolation

Each `EditorTab` creates a UUID-named subdirectory under `temp/` at construction time.
`Compiler` cleans `.java` and `.class` files from this directory before each run, writes the
new source, and compiles into it. `Executor` sets this directory as the classpath root.
When the tab is closed, the temp directory is deleted entirely.

### Output streaming

`Executor` merges stderr into stdout using `ProcessBuilder.redirectErrorStream(true)` and
reads the combined stream one byte at a time. Reading by character rather than by line means
output like `Enter your name:` appears in the console immediately — the prompt does not wait
for a newline before becoming visible.

### Syntax highlighting

`SyntaxHighlighter.computeHighlighting()` runs a single compiled `Pattern` against the full
source text and returns a `StyleSpans` object that RichTextFX applies to the `CodeArea`.
Token types handled: `keyword`, `string`, `comment`, `annotation`, `number`.

Highlighting is triggered by a `PauseTransition` (150 ms debounce) on every text change, so
it computes once after the user stops typing rather than on every keystroke.

---

## Tech stack

| Component | Technology |
|---|---|
| Language | Java 25 |
| UI framework | JavaFX 25 |
| Code editor component | RichTextFX 0.11.2 (CodeArea) |
| Build tool | Maven with javafx-maven-plugin |
| Compilation subprocess | javac via ProcessBuilder |
| Execution subprocess | java via ProcessBuilder |

---

## Getting started

### Prerequisites

- JDK 25 (or the version matching `pom.xml`)
- Maven 3.8+

### Build and run

```shell
git clone https://github.com/dhruvrajsinghgaur/CodeExecutionEngine.git
cd CodeExecutionEngine
mvn clean javafx:run
```

### Using the IDE

1. **Write** Java code in the editor — the class name is extracted automatically from the
   `public class` (or `interface`, `enum`, `record`) declaration
2. **Click Run** — the status bar shows `Compiling...` then switches to running state
3. **Type in the input field** at the bottom and press Enter to send stdin to the program
4. **Click Stop** to kill a running or hung process
5. **Click Clear** to empty the output console
6. **New / Open / Save / Save As** work per-tab via the toolbar

---

## Project structure

```
CodeExecutionEngine/
├── src/
│   └── main/
│       ├── java/
│       │   ├── IDE.java                  application entry point, toolbar, tab management
│       │   ├── EditorTab.java            per-tab component: editor, output, stdin
│       │   ├── Compiler.java             javac subprocess wrapper
│       │   ├── Executor.java             java subprocess wrapper and IO thread
│       │   └── SyntaxHighlighter.java    regex-based Java syntax highlighter
│       └── resources/
│           └── style.css                 editor, output area, and token colour styles
├── temp/                                 runtime working dirs (gitignored)
├── .gitignore
└── pom.xml
```

---

## Known limitations

- **No execution timeout** — a program with an infinite loop runs until Stop is clicked
- **Single-file only** — multi-class projects spanning separate source files are not supported
- **High-volume output lag** — output is forwarded to the UI per character via
  `Platform.runLater`; programs printing large amounts of data rapidly may cause UI lag
- **Temp dir not cleaned on crash** — if the JVM exits abnormally, UUID subdirectories
  under `temp/` may be left on disk

---

## Author

**Dhruvraj Singh Gaur**  
B.Tech CSE Student  
[GitHub](https://github.com/dhruvrajsinghgaur)