import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while",

            "var", "record", "sealed", "permits", "yield", "when"
    };

    private static final String COMMENT_PATTERN    = "//[^\n]*|/\\*.*?\\*/";

    private static final String STRING_PATTERN     = "\"([^\"\\\\]|\\\\.)*\"";

    private static final String CHAR_PATTERN       = "'([^'\\\\]|\\\\.)'";

    private static final String ANNOTATION_PATTERN = "@[\\w]+";

    private static final String KEYWORD_PATTERN    =
            "\\b(" + String.join("|", KEYWORDS) + ")\\b";

    private static final String NUMBER_PATTERN     =
            "\\b(0[xX][0-9a-fA-F]+[lL]?|\\d+\\.?\\d*([eE][+-]?\\d+)?[fFdDlL]?)\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>"     + COMMENT_PATTERN    + ")"
                    + "|(?<STRING>"    + STRING_PATTERN     + ")"
                    + "|(?<CHAR>"      + CHAR_PATTERN       + ")"
                    + "|(?<ANNOTATION>"+ ANNOTATION_PATTERN + ")"
                    + "|(?<KEYWORD>"   + KEYWORD_PATTERN    + ")"
                    + "|(?<NUMBER>"    + NUMBER_PATTERN     + ")",
            Pattern.DOTALL
    );


    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);

            String styleClass =
                    matcher.group("COMMENT")    != null ? "comment"    :
                            matcher.group("STRING")     != null ? "string"     :
                            matcher.group("CHAR")       != null ? "string"     :
                            matcher.group("ANNOTATION") != null ? "annotation" :
                            matcher.group("KEYWORD")    != null ? "keyword"    :
                            matcher.group("NUMBER")     != null ? "number"     :
                            null;

            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }
}