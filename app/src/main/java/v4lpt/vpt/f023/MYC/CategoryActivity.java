package v4lpt.vpt.f023.MYC;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CategoryActivity extends AppCompatActivity
        implements PoemAdapter.OnIdChangeListener,
        PoemAdapter.OnPoemClickListener,
        PoemAdapter.OnPoemDeleteListener {

    private PoemAdapter adapter;
    private IdManager idManager;
    private MaterialButton btnEditMode;
    private MaterialButton fabAdd;
    private MaterialButton btnFileImport;
    private File memorizeDir;
    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) {
            finish();
            return;
        }

        memorizeDir = new File(getExternalFilesDir(null), "Memorizer");
        idManager = new IdManager(this);

        setupViews();
        loadItems();
    }

    private void setupViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PoemAdapter(this, this, this, this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final float DELETE_THRESHOLD = 0.7f;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                IdManager.FileItem item = adapter.getItemAt(position);
                onPoemDelete(item);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return DELETE_THRESHOLD;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 3f;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float width = itemView.getWidth();
                float swipeProgress = Math.abs(dX) / width;

                // Draw background
                Paint paint = new Paint();
                if (swipeProgress >= DELETE_THRESHOLD) {
                    // Normal red when past threshold (swapped)
                    paint.setColor(Color.RED);
                } else {
                    // Darker red before threshold (swapped)
                    paint.setColor(Color.rgb(180, 0, 0));
                }

                RectF background = new RectF(
                        itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom()
                );
                c.drawRect(background, paint);

                // Calculate available width for text
                float availableWidth = Math.abs(dX);

                // Setup text paint
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.MONOSPACE);

                // Get the text to display
                String text = swipeProgress >= DELETE_THRESHOLD ? "Delete!!!" : "Delete?";

                // Start with initial text size
                float textSize = 80f;
                paint.setTextSize(textSize);

                // Measure text width and scale down if needed
                Rect textBounds = new Rect();
                paint.getTextBounds(text, 0, text.length(), textBounds);

                // Scale text size to fit within available width with padding
                float padding = 40f; // Padding on each side
                if (textBounds.width() > (availableWidth - padding * 2)) {
                    textSize *= (availableWidth - padding * 2) / textBounds.width();
                    paint.setTextSize(textSize);
                }

                // Set alpha based on swipe progress
                if (swipeProgress >= DELETE_THRESHOLD) {
                    paint.setAlpha(255);
                } else {
                    paint.setAlpha((int)(255 * (swipeProgress / DELETE_THRESHOLD)));
                }

                // Draw text only if we have enough space
                if (availableWidth > padding * 2) {
                    float textX = itemView.getRight() - (availableWidth / 2);
                    float textY = itemView.getTop() + ((itemView.getBottom() - itemView.getTop()) / 2f) +
                            (textBounds.height() / 2f);
                    c.drawText(text, textX, textY, paint);
                }

                itemView.setTranslationX(dX);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);



        btnEditMode = findViewById(R.id.btnEditMode);
        btnEditMode.setOnClickListener(v -> toggleEditMode());

        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportActivity.class);
            intent.putExtra("SELECTED_CATEGORY", category);
            startActivity(intent);
        });

        btnFileImport = findViewById(R.id.btnFileImport);
        btnFileImport.setOnClickListener(v -> {
            Intent intent = new Intent(this, FileImportActivity.class);
            intent.putExtra("SELECTED_CATEGORY", category);
            startActivity(intent);
        });
    }

    private void toggleEditMode() {
        boolean newEditMode = !btnEditMode.isSelected();
        btnEditMode.setSelected(newEditMode);

        // Change icon based on selection state instead of background color
        btnEditMode.setIconResource(newEditMode ?
                R.drawable.numbers_fill : R.drawable.numbers_notfill);

        if (!newEditMode) {
            adapter.finalizeEditing();
        } else {
            // Hide keyboard first if it's visible
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
            adapter.setEditMode(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    @Override
    public void onIdsChanged(Map<Integer, Integer> oldToNewIds) {
        // Implementiere hier die Logik zum Umbenennen der Dateien mit den neuen IDs
        List<IdManager.FileItem> allFiles = idManager.getAllFiles(memorizeDir);
        for (IdManager.FileItem item : allFiles) {
            Integer newId = oldToNewIds.get(item.getId());
            if (newId != null) {
                File oldFile = item.getFile();
                File newFile = new File(oldFile.getParent(),
                        newId + "_" + oldFile.getName().split("_", 2)[1]);
                if (!oldFile.equals(newFile)) {
                    oldFile.renameTo(newFile);
                }
            }
        }

        // Liste neu laden und sortieren
        loadItems();
    }

    private void loadItems() {
        List<IdManager.FileItem> items = idManager.getAllFiles(memorizeDir);
        // Filtere nach Kategorie
        items = items.stream()
                .filter(item -> item.getCategory().equals(category))
                .collect(Collectors.toList());
        Collections.sort(items, (a, b) -> Integer.compare(a.getId(), b.getId()));
        adapter.updateItems(items);
    }

    @Override
    public void onPoemDelete(IdManager.FileItem item) {
        // Delete the file
        if (item.getFile().delete()) {
            // Refresh the list after successful deletion
            loadItems();
            Toast.makeText(this, "Poem deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete poem", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPoemClick(IdManager.FileItem item) {
        Intent intent = new Intent(this, ViewActivity.class);
        intent.putExtra("FILE_PATH", item.getFile().getAbsolutePath());
        intent.putExtra("GLOBAL_ID", item.getId());
        intent.putExtra("CATEGORY", item.getCategory());
        startActivity(intent);
    }
}