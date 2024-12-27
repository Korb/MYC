package v4lpt.vpt.f023.MYC;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LineHeightSpan;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class LineNumberedTextView extends AppCompatTextView {
    private Paint lineNumberPaint;
    private int lineNumberWidth;
    private int startLineNumber = 1;
    private static final String INDENTATION_MARKER = "    ";
    private static final int LINE_NUMBER_MARGIN = 70;
    private static final float VERTICAL_OFFSET = 1.87f;
    private static final float LINE_NUMBER_SIZE_RATIO = 0.5f;

    public LineNumberedTextView(Context context) {
        super(context);
        init();
    }

    public LineNumberedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize line number paint
        lineNumberPaint = new TextPaint();
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        lineNumberPaint.setTextSize(getTextSize() * LINE_NUMBER_SIZE_RATIO);
        lineNumberPaint.setColor(0xFF888888);
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);

        // Calculate width needed for line numbers plus margin
        lineNumberWidth = (int) (lineNumberPaint.measureText("999") + LINE_NUMBER_MARGIN);

        // Add padding to make room for line numbers
        setPadding(lineNumberWidth + getPaddingLeft(), getPaddingTop(),
                getPaddingRight(), getPaddingBottom());
    }

    public void setStartLineNumber(int startNumber) {
        this.startLineNumber = startNumber;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineCount = getLineCount();
        int currentRealLine = startLineNumber;

        for (int i = 0; i < lineCount; i++) {
            int[] boundaries = getLineBoundaries(i);
            int lineTop = boundaries[0];
            int lineBottom = boundaries[2];
            String lineText = getLineText(i);

            // First check if it's a display line (indented) without trimming
            boolean isDisplayLine = lineText.startsWith(INDENTATION_MARKER);
            // Then check if it's empty after trimming
            boolean isEmpty = lineText.trim().isEmpty();

            // Only draw line number if this is not a display line and not empty
            if (!isDisplayLine && !isEmpty) {
                String lineNumber = String.valueOf(currentRealLine);

                float lineCenter = (lineTop + lineBottom) / 2.0f;
                float offset = lineNumberPaint.getTextSize() * VERTICAL_OFFSET;

                canvas.drawText(lineNumber, getPaddingLeft() - LINE_NUMBER_MARGIN, lineCenter + offset, lineNumberPaint);
                currentRealLine++;
            }
        }

        super.onDraw(canvas);
    }

    private String getLineText(int lineNumber) {
        Layout layout = getLayout();
        if (layout == null) return "";

        int lineStart = layout.getLineStart(lineNumber);
        int lineEnd = layout.getLineEnd(lineNumber);
        return getText().subSequence(lineStart, lineEnd).toString();
    }

    private int[] getLineBoundaries(int line) {
        Rect bounds = new Rect();
        Layout layout = getLayout();

        int baseline = layout.getLineBaseline(line);
        layout.getLineBounds(line, bounds);

        return new int[]{ bounds.top, baseline, bounds.bottom };
    }
}