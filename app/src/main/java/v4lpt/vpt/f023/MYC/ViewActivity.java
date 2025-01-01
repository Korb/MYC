package v4lpt.vpt.f023.MYC;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class ViewActivity extends Activity implements View.OnTouchListener {
    private CustomTextPaginator mainPaginator;
    private CustomTextPaginator contextPaginator;
    private List<String> pages;
    private int currentPage = 0;
    private TextView titleYearView;
    private TextView pageNumberView;
    private TextView authorView;
    private TextView contentView;
    private TextView prevContextView;
    private TextView nextContextView;
    private float x1, x2;
    static final int MIN_DISTANCE = 150;
    private static final int CONTEXT_REAL_LINES = 2; // Number of real lines to show in context
    private static final int PREV_CONTEXT_COLOR = R.color.vptrose;
    private static final int NEXT_CONTEXT_COLOR = R.color.vptdarkerrose;
    private static final String PLACEHOLDER_TEXT = "\n";
    private String title;
    private String author;
    private String year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        titleYearView = findViewById(R.id.titleYearView);
        pageNumberView = findViewById(R.id.pageNumberView);
        authorView = findViewById(R.id.authorView);
        contentView = findViewById(R.id.contentView);
        prevContextView = findViewById(R.id.prevContextView);
        nextContextView = findViewById(R.id.nextContextView);

        // Set monospace font
        Typeface monospace = Typeface.MONOSPACE;
        titleYearView.setTypeface(monospace);
        pageNumberView.setTypeface(monospace);
        authorView.setTypeface(monospace);
        contentView.setTypeface(monospace);
        prevContextView.setTypeface(monospace);
        nextContextView.setTypeface(monospace);

        String filePath = getIntent().getStringExtra("FILE_PATH");
        int globalId = getIntent().getIntExtra("GLOBAL_ID", -1);
        String category = getIntent().getStringExtra("CATEGORY");

        setTitle(category + " - " + globalId);

        // Initialize main paginator
        TextPaint mainTextPaint = new TextPaint();
        mainTextPaint.setTextSize(contentView.getTextSize());
        mainTextPaint.setTypeface(monospace);
        int mainMaxWidth = getResources().getDisplayMetrics().widthPixels -
                (contentView.getPaddingLeft() + contentView.getPaddingRight());
        mainPaginator = new CustomTextPaginator(mainTextPaint, mainMaxWidth, 20);

        // Initialize context paginator
        TextPaint contextTextPaint = new TextPaint();
        contextTextPaint.setTextSize(prevContextView.getTextSize());
        contextTextPaint.setTypeface(monospace);
        int contextMaxWidth = getResources().getDisplayMetrics().widthPixels -
                (prevContextView.getPaddingLeft() + prevContextView.getPaddingRight());
        contextPaginator = new CustomTextPaginator(contextTextPaint, contextMaxWidth, 2); // 2 lines for context

        // Load and paginate the text
        String content = loadFileContent(filePath);
        pages = mainPaginator.paginateText(content);

        // Display first page
        displayPage(currentPage);

        // Set touch listener
        View touchArea = findViewById(R.id.touchArea);
        touchArea.setOnTouchListener(this);
        hideSystemUI();
    }

    private void displayPage(int pageIndex) {
        // Calculate starting line number for this page
        int startLineNumber = 1;
        for (int i = 0; i < pageIndex; i++) {
            // Count real lines in previous pages
            startLineNumber += countRealLines(pages.get(i));
        }

        ((LineNumberedTextView) contentView).setStartLineNumber(startLineNumber);
        contentView.setText(pages.get(pageIndex));
        updateTitleYearView();
        updatePageNumberView(pageIndex);
        updateAuthorView();
        updateContextViews(pageIndex);
    }

    private void updateTitleYearView() {
        String titleYearText = title + " (" + year + ")";
        titleYearView.setText(titleYearText);
    }

    private void updatePageNumberView(int pageIndex) {
        String pageInfo = String.format("%02d/%02d", pageIndex + 1, pages.size());
        pageNumberView.setText(pageInfo);
    }

    private void updateAuthorView() {
        authorView.setText(author);
    }



    private void updateContextViews(int pageIndex) {
        // Previous context
        if (pageIndex > 0) {
            String prevPage = pages.get(pageIndex - 1);
            String prevContext = getLastDisplayLines(prevPage, 2);
            SpannableString spannablePrev = new SpannableString(prevContext);
            spannablePrev.setSpan(new ForegroundColorSpan(getResources().getColor(PREV_CONTEXT_COLOR)),
                    0, spannablePrev.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            prevContextView.setText(spannablePrev);
        } else {
            prevContextView.setText(PLACEHOLDER_TEXT);
        }

        // Next context
        if (pageIndex < pages.size() - 1) {
            String nextPage = pages.get(pageIndex + 1);
            String nextContext = getFirstDisplayLines(nextPage, 2);
            SpannableString spannableNext = new SpannableString(nextContext);
            spannableNext.setSpan(new ForegroundColorSpan(getResources().getColor(NEXT_CONTEXT_COLOR)),
                    0, spannableNext.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            nextContextView.setText(spannableNext);
        } else {
            nextContextView.setText(PLACEHOLDER_TEXT);
        }
    }

    private String getFirstDisplayLines(String text, int lineCount) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(lineCount, lines.length); i++) {
            result.append(lines[i]);
            if (i < Math.min(lineCount - 1, lines.length - 1)) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private String getLastDisplayLines(String text, int lineCount) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();

        int startIndex = Math.max(0, lines.length - lineCount);
        for (int i = startIndex; i < lines.length; i++) {
            result.append(lines[i]);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (x2 > x1) {
                        // Swipe right to left (previous page)
                        if (currentPage > 0) {
                            currentPage--;
                            displayPage(currentPage);
                        }
                    } else {
                        // Swipe left to right (next page)
                        if (currentPage < pages.size() - 1) {
                            currentPage++;
                            displayPage(currentPage);
                        }
                    }
                } else {
                    // It was a click
                    if (event.getX() < getResources().getDisplayMetrics().widthPixels / 2) {
                        // Left side click (previous page)
                        if (currentPage > 0) {
                            currentPage--;
                            displayPage(currentPage);
                        }
                    } else {
                        // Right side click (next page)
                        if (currentPage < pages.size() - 1) {
                            currentPage++;
                            displayPage(currentPage);
                        }
                    }
                }
                return true;
        }
        return false;
    }

    private String loadFileContent(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            // Read the first three lines (author, title, year)
            author = reader.readLine();
            title = reader.readLine();
            year = reader.readLine();

            // Skip until we find the "start_content" line
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("start_content")) {
                    break;
                }
            }

            // Read the actual content
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
    private int countRealLines(String pageText) {
        return (int) pageText.lines()
                .filter(line -> !line.startsWith("    "))
                .count();
    }
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Adjust the layout params of the rating layout
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Reset the layout params
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags &= ~(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setAttributes(params);
    }
    protected void onDestroy() {
        // Restore system UI before destroying
        showSystemUI();
        super.onDestroy();
    }
}