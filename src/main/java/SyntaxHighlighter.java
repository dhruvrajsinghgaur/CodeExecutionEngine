import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    // ── Token definitions ─────────────────────────────────────────────────────────

    private static final String[] KEYWORDS = {
            // Primitive types and modifiers
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while",
            // Modern Java keywords (Java 14+)
            "var", "record", "sealed", "permits", "yield", "when"
    };

    // Comments before strings/keywords so "// public" doesn't keyword-highlight
    private static final String COMMENT_PATTERN    = "//[^\n]*|/\\*.*?\\*/";

    // Strings before keywords so "\"public\"" doesn't trigger keyword highlighting
    private static final String STRING_PATTERN     = "\"([^\"\\\\]|\\\\.)*\"";

    // Char literals (e.g. 'a', '\n', '\\')
    private static final String CHAR_PATTERN       = "'([^'\\\\]|\\\\.)'";

    // Annotations before keywords so @interface isn't parsed as "interface" keyword
    private static final String ANNOTATION_PATTERN = "@[\\w]+";

    // Keywords (exact word boundaries to avoid matching "integer" for "int")
    private static final String KEYWORD_PATTERN    =
            "\\b(" + String.join("|", KEYWORDS) + ")\\b";

    // Number literals: hex (0xFF), decimal, float (3.14f), long (100L), scientific (1e5)
    private static final String NUMBER_PATTERN     =
            "\\b(0[xX][0-9a-fA-F]+[lL]?|\\d+\\.?\\d*([eE][+-]?\\d+)?[fFdDlL]?)\\b";

    // Pattern order matters — earlier groups shadow later ones at the same position.
    // Comments → Strings → Chars → Annotations → Keywords → Numbers
    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>"     + COMMENT_PATTERN    + ")"
                    + "|(?<STRING>"    + STRING_PATTERN     + ")"
                    + "|(?<CHAR>"      + CHAR_PATTERN       + ")"
                    + "|(?<ANNOTATION>"+ ANNOTATION_PATTERN + ")"
                    + "|(?<KEYWORD>"   + KEYWORD_PATTERN    + ")"
                    + "|(?<NUMBER>"    + NUMBER_PATTERN     + ")",
            Pattern.DOTALL   // makes . match newlines → multi-line /* */ comments work
    );

    // ── Public API ────────────────────────────────────────────────────────────────

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            // Unstyled gap between the previous match and this one
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);

            // Determine which group matched and map it to a CSS class
            String styleClass =
                    matcher.group("COMMENT")    != null ? "comment"    :
                            matcher.group("STRING")     != null ? "string"     :
                            matcher.group("CHAR")       != null ? "string"     : // reuse string colour
                            matcher.group("ANNOTATION") != null ? "annotation" :
                            matcher.group("KEYWORD")    != null ? "keyword"    :
                            matcher.group("NUMBER")     != null ? "number"     :
                            null;

            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        // Trailing unstyled text
        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }
}