package v4lpt.vpt.f023.MYC;
import android.text.TextPaint;
import java.util.ArrayList;
import java.util.List;

public class CustomTextPaginator {
    private final TextPaint textPaint;
    private final int maxWidth;
    private final int maxLinesPerPage;
    private final String wrapIndentation = "    ";

    public CustomTextPaginator(TextPaint textPaint, int maxWidth, int maxLinesPerPage) {
        this.textPaint = textPaint;
        this.maxWidth = maxWidth;
        this.maxLinesPerPage = maxLinesPerPage;
    }

    public List<String> paginateText(String text) {
        List<String> pages = new ArrayList<>();
        String[] stanzas = text.split("\n\\s*\n");

        List<String> currentPageStanzas = new ArrayList<>();
        int currentDisplayLines = 0;

        for (int i = 0; i < stanzas.length; i++) {
            List<String> formattedStanzaLines = formatStanza(stanzas[i]);
            int stanzaDisplayLines = formattedStanzaLines.size();

            // If single stanza is larger than maxLinesPerPage, split it
            if (stanzaDisplayLines > maxLinesPerPage) {
                // First flush any accumulated stanzas to a new page
                if (!currentPageStanzas.isEmpty()) {
                    pages.add(String.join("\n\n", currentPageStanzas));
                    currentPageStanzas.clear();
                    currentDisplayLines = 0;
                }

                // Split the large stanza
                pages.addAll(splitStanzaEvenly(formattedStanzaLines));
                continue;
            }

            // Calculate total display lines if we add this stanza
            int blankLineCount = currentPageStanzas.isEmpty() ? 0 : 1;
            int newTotalDisplayLines = currentDisplayLines + blankLineCount + stanzaDisplayLines;

            if (newTotalDisplayLines > maxLinesPerPage) {
                // Current stanza won't fit, flush accumulated stanzas to new page
                if (!currentPageStanzas.isEmpty()) {
                    pages.add(String.join("\n\n", currentPageStanzas));
                    currentPageStanzas.clear();
                }

                // Start new page with current stanza
                currentPageStanzas.add(String.join("\n", formattedStanzaLines));
                currentDisplayLines = stanzaDisplayLines;
            } else {
                // Add stanza to current page
                currentPageStanzas.add(String.join("\n", formattedStanzaLines));
                currentDisplayLines = newTotalDisplayLines;
            }

            // Handle last stanza
            if (i == stanzas.length - 1 && !currentPageStanzas.isEmpty()) {
                pages.add(String.join("\n\n", currentPageStanzas));
            }
        }

        return pages;
    }

    private List<String> splitStanzaEvenly(List<String> stanzaLines) {
        List<String> splitPages = new ArrayList<>();
        int totalLines = stanzaLines.size();
        int numPages = (int) Math.ceil((double) totalLines / maxLinesPerPage);

        // Calculate lines per page
        int baseLines = totalLines / numPages;
        int remainingLines = totalLines % numPages;

        int startIdx = 0;
        for (int i = 0; i < numPages; i++) {
            int linesForThisPage = baseLines + (remainingLines > 0 ? 1 : 0);
            remainingLines--;

            List<String> pageLines = stanzaLines.subList(startIdx,
                    Math.min(startIdx + linesForThisPage, totalLines));
            splitPages.add(String.join("\n", pageLines));
            startIdx += linesForThisPage;
        }

        return splitPages;
    }

    private List<String> formatStanza(String stanza) {
        List<String> formattedLines = new ArrayList<>();
        String[] lines = stanza.trim().split("\n");

        for (String line : lines) {
            formattedLines.addAll(wrapLine(line.trim()));
        }

        return formattedLines;
    }

    private List<String> wrapLine(String line) {
        List<String> wrappedLines = new ArrayList<>();
        String[] words = line.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        boolean isFirstLine = true;

        for (String word : words) {
            String testLine = currentLine.toString();
            if (testLine.length() > 0) {
                testLine += " ";
            }
            testLine += word;

            float lineWidth = textPaint.measureText(isFirstLine ? testLine : wrapIndentation + testLine);

            if (lineWidth <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    wrappedLines.add(isFirstLine ? currentLine.toString() : wrapIndentation + currentLine.toString());
                    currentLine = new StringBuilder(word);
                    isFirstLine = false;
                } else {
                    wrappedLines.add(isFirstLine ? word : wrapIndentation + word);
                    isFirstLine = false;
                }
            }
        }

        if (currentLine.length() > 0) {
            wrappedLines.add(isFirstLine ? currentLine.toString() : wrapIndentation + currentLine.toString());
        }

        return wrappedLines;
    }
}