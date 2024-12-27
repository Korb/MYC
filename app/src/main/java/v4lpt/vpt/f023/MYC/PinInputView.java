package v4lpt.vpt.f023.MYC;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.AppCompatEditText;
import android.graphics.Color;

public class PinInputView extends AppCompatEditText {
    private Paint paint;
    private Rect[] digitRects;
    private int currentPosition = 0;
    private OnPinCompleteListener onPinCompleteListener;
    private boolean isBlinking = false;
    private boolean hasFocus = false;
    private final int DIGIT_SPACING = 20;
    private final int UNDERLINE_WIDTH = 80;
    private final float TEXT_SIZE = 120f;
    private final float INSTRUCTION_TEXT_SIZE = 48f;
    private final float CURSOR_HEIGHT_PERCENTAGE = 0.9f;
    private final int TEXT_COLOR = Color.WHITE;
    private final int TEXT_COLOR2 = getContext().getResources().getColor(R.color.notquitewhite);
    private final String INSTRUCTION_TEXT = "Quick draw:\nenter poem code (3 digits)";
    private Rect textBounds;
    private TextPaint instructionPaint;

    public interface OnPinCompleteListener {
        void onPinComplete(String pin);
    }

    public PinInputView(Context context) {
        super(context);
        init();
    }

    public PinInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PinInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(TEXT_COLOR);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(TEXT_SIZE);

        instructionPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        instructionPaint.setColor(TEXT_COLOR2);
        instructionPaint.setTextSize(INSTRUCTION_TEXT_SIZE);
        instructionPaint.setTypeface(Typeface.MONOSPACE);
        instructionPaint.setTextAlign(Paint.Align.CENTER);

        digitRects = new Rect[3];
        for (int i = 0; i < 3; i++) {
            digitRects[i] = new Rect();
        }
        textBounds = new Rect();

        setInputType(InputType.TYPE_CLASS_NUMBER);
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        setTextColor(Color.TRANSPARENT);

        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        });

        addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPosition = s.length();
                if (currentPosition == 3 && onPinCompleteListener != null) {
                    onPinCompleteListener.onPinComplete(s.toString());
                }
                invalidate();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        post(new Runnable() {
            @Override
            public void run() {
                isBlinking = !isBlinking;
                invalidate();
                postDelayed(this, 500);
            }
        });
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        hasFocus = focused;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!hasFocus) {
            // Draw instruction text
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;

            String[] lines = INSTRUCTION_TEXT.split("\n");
            float lineHeight = instructionPaint.getFontSpacing();
            float totalHeight = lineHeight * lines.length;
            float startY = centerY - (totalHeight / 2) + instructionPaint.getTextSize();

            for (String line : lines) {
                canvas.drawText(line, centerX, startY, instructionPaint);
                startY += lineHeight;
            }
            return;
        }

        paint.setTextSize(TEXT_SIZE);

        // Calculate exact text dimensions for a sample digit
        paint.getTextBounds("0", 0, 1, textBounds);
        int digitHeight = textBounds.height();

        // Calculate total width needed for digits
        float totalWidth = (3 * UNDERLINE_WIDTH) + (2 * DIGIT_SPACING);

        // Calculate vertical center
        float verticalCenter = getHeight() / 2f;

        // Calculate the baseline for perfect vertical centering
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = verticalCenter - (fm.descent + fm.ascent) / 2;

        // Calculate cursor dimensions
        float textHeight = Math.abs(fm.ascent - fm.descent);
        float cursorHeight = textHeight * CURSOR_HEIGHT_PERCENTAGE;
        float cursorTopOffset = (textHeight - cursorHeight) / 2;

        // Calculate cursor bottom position - this will be used for the underline position too
        float cursorBottom = baseline + fm.ascent + cursorTopOffset + cursorHeight;

        // Calculate horizontal starting position
        int startX = (getWidth() - (int)totalWidth) / 2;

        for (int i = 0; i < 3; i++) {
            int digitLeft = startX + (i * (UNDERLINE_WIDTH + DIGIT_SPACING));

            // Draw digit if entered
            if (i < getText().length()) {
                String digit = String.valueOf(getText().charAt(i));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT_COLOR);
                float textWidth = paint.measureText(digit);
                canvas.drawText(
                        digit,
                        digitLeft + (UNDERLINE_WIDTH - textWidth) / 2,
                        baseline,
                        paint
                );
            }
            // Draw either cursor block or underscore for empty position
            else if (i == currentPosition) {
                if (isBlinking) {
                    // Draw shorter cursor block
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(TEXT_COLOR);
                    canvas.drawRect(
                            digitLeft,
                            baseline + fm.ascent + cursorTopOffset,
                            digitLeft + UNDERLINE_WIDTH,
                            cursorBottom,
                            paint
                    );
                } else {
                    // Draw underscore when cursor is not showing
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(TEXT_COLOR);
                    canvas.drawLine(
                            digitLeft,
                            cursorBottom,
                            digitLeft + UNDERLINE_WIDTH,
                            cursorBottom,
                            paint
                    );
                }
            }
            // Draw underscore for remaining empty positions
            else if (i > currentPosition) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(TEXT_COLOR);
                canvas.drawLine(
                        digitLeft,
                        cursorBottom,
                        digitLeft + UNDERLINE_WIDTH,
                        cursorBottom,
                        paint
                );
            }
        }
    }

    public void setOnPinCompleteListener(OnPinCompleteListener listener) {
        this.onPinCompleteListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        paint.setTextSize(TEXT_SIZE);
        Paint.FontMetrics fm = paint.getFontMetrics();
        int desiredHeight = (int)(Math.abs(fm.ascent - fm.descent) * 2);  // Double the text height for padding

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }
}