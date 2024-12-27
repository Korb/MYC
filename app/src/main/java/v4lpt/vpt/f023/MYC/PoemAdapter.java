package v4lpt.vpt.f023.MYC;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PoemAdapter extends RecyclerView.Adapter<PoemAdapter.ViewHolder> {
    private List<IdManager.FileItem> items;
    private boolean isEditMode = false;
    private ViewHolder currentEditingHolder = null;
    private final Context context;
    private final OnIdChangeListener idChangeListener;
    private final OnPoemClickListener poemClickListener;
    private Map<Integer, String> tempNewIds = new HashMap<>();
    private boolean isUserInput = false;
    private boolean isNavigating = false;
    public static interface OnIdChangeListener {
        void onIdsChanged(Map<Integer, Integer> oldToNewIds);
    }

    public static interface OnPoemClickListener {
        void onPoemClick(IdManager.FileItem item);
    }


    private final OnPoemDeleteListener poemDeleteListener;

    public interface OnPoemDeleteListener {
        void onPoemDelete(IdManager.FileItem item);
    }

    public PoemAdapter(Context context, OnIdChangeListener idChangeListener,
                       OnPoemClickListener poemClickListener,
                       OnPoemDeleteListener poemDeleteListener) {
        this.context = context;
        this.idChangeListener = idChangeListener;
        this.poemClickListener = poemClickListener;
        this.poemDeleteListener = poemDeleteListener;
        this.items = new ArrayList<>();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();

        if (editMode && !items.isEmpty()) {
            new android.os.Handler().postDelayed(() -> {
                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    RecyclerView recyclerView = activity.findViewById(R.id.recyclerView);
                    if (recyclerView != null) {
                        ViewHolder firstHolder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(0);
                        if (firstHolder != null) {
                            firstHolder.globalIdView.requestFocus();
                            showKeyboard(firstHolder.globalIdView);
                            currentEditingHolder = firstHolder;
                        }
                    }
                }
            }, 150);
        } else {
            if (currentEditingHolder != null) {
                hideKeyboard(currentEditingHolder.globalIdView);
                currentEditingHolder.globalIdView.clearFocus();
            }
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IdManager.FileItem item = items.get(position);

        // ID Setup
        String formattedId = String.format("%03d", item.getId());
        holder.globalIdView.setHint(formattedId);
        holder.globalIdView.setText(""); // Clear the text
        holder.globalIdView.setEnabled(isEditMode);
        if (isEditMode) {
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setOnClickListener(v -> {
                if (poemClickListener != null) {
                    poemClickListener.onPoemClick(item);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (poemDeleteListener != null) {
                    androidx.appcompat.app.AlertDialog.Builder builder =
                            new androidx.appcompat.app.AlertDialog.Builder(context, R.style.AlertDialogTheme);
                    builder.setTitle("Delete")
                            .setMessage("Do you want to delete this poem?")
                            .setPositiveButton("Yes", (dialog, which) ->
                                    poemDeleteListener.onPoemDelete(item))
                            .setNegativeButton("No", (dialog, which) ->
                                    dialog.dismiss())
                            .show();
                }
                return true;
            });
        }
        // Only set up the TextWatcher if it hasn't been set up yet
        if (holder.getTextWatcher() == null) {
            TextWatcher watcher = new TextWatcher() {
                private String beforeText = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    beforeText = s.toString();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isNavigating) return;  // Skip if we're already navigating

                    // Only proceed if text is actually being added (not cleared or set programmatically)
                    if (s.length() == 3 && s.length() > beforeText.length()) {
                        try {
                            int newId = Integer.parseInt(s.toString());
                            handleIdChange(holder.getAdapterPosition(), s.toString());
                            isNavigating = true;  // Set flag before navigation
                            new Handler().postDelayed(() -> {
                                moveToNextItem(holder.getAdapterPosition());
                                isNavigating = false;  // Reset flag after navigation
                            }, 100);
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
            holder.setTextWatcher(watcher);
            holder.globalIdView.addTextChangedListener(watcher);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(item.getFile()));
            String author = reader.readLine();
            String title = reader.readLine();
            holder.titleView.setText(title);
            holder.authorView.setText(author);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            holder.titleView.setText("Error");
            holder.authorView.setText("Error loading file");
        }

        setupEditTextListeners(holder, position);
    }


    private void setupEditTextListeners(ViewHolder holder, int position) {
        holder.globalIdView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String currentText = holder.globalIdView.getText().toString();
                if (!currentText.isEmpty()) {
                    handleIdChange(position, currentText);
                }
                hideKeyboard(holder.globalIdView);
                holder.globalIdView.clearFocus();
                return true;
            }
            return false;
        });

        holder.globalIdView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                currentEditingHolder = holder;
                showKeyboard(holder.globalIdView);
            }
        });
    }
    private void moveToNextItem(int currentPosition) {
        if (currentPosition < items.size() - 1) {
            RecyclerView recyclerView = (RecyclerView) currentEditingHolder.itemView.getParent();
            if (recyclerView != null) {
                int nextPosition = currentPosition + 1;

                // First scroll to make the next item visible
                recyclerView.smoothScrollToPosition(nextPosition);

                // Wait for scroll to complete and then focus the next item
                new Handler().postDelayed(() -> {
                    ViewHolder nextHolder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(nextPosition);
                    if (nextHolder != null) {
                        nextHolder.globalIdView.requestFocus();
                        showKeyboard(nextHolder.globalIdView);
                        currentEditingHolder = nextHolder;
                    }
                }, 100);
            }
        } else {
            // At last item
            hideKeyboard(currentEditingHolder.globalIdView);
            currentEditingHolder.globalIdView.clearFocus();
        }
    }



    private void handleIdChange(int position, String newIdStr) {
        int newId = Integer.parseInt(newIdStr);
        IdManager.FileItem currentItem = items.get(position);

        // Suche nach einem Item mit der gleichen ID
        for (int i = 0; i < items.size(); i++) {
            if (i != position && items.get(i).getId() == newId) {
                // ID existiert bereits, also tauschen wir sie
                int oldId = currentItem.getId();

                // Finde den ViewHolder für das andere Item
                RecyclerView recyclerView = (RecyclerView) currentEditingHolder.itemView.getParent();
                if (recyclerView != null) {
                    ViewHolder otherHolder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                    if (otherHolder != null) {
                        // Aktualisiere die Anzeige für das andere Item
                        otherHolder.globalIdView.setHint(String.format("%03d", oldId));
                        otherHolder.globalIdView.setText("");
                    }
                }

                // Aktualisiere die temporären IDs für beide Items
                tempNewIds.put(currentItem.getId(), String.format("%03d", newId));
                tempNewIds.put(items.get(i).getId(), String.format("%03d", oldId));

                // Zeige eine Nachricht über den Tausch an
                String message = "Swapped IDs " + newId + " and " + oldId;
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Wenn keine Kollision, speichere einfach die neue ID
        tempNewIds.put(currentItem.getId(), newIdStr);
    }
    private void showKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        editText.postDelayed(() -> {
            editText.requestFocus();
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }

    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public void finalizeEditing() {
        if (!isEditMode) return;

        // Erste Phase: Sammle alle vorgeschlagenen IDs
        Map<Integer, Integer> oldToNewIds = new HashMap<>();
        Set<Integer> usedNewIds = new HashSet<>();
        Map<Integer, Integer> duplicateIds = new HashMap<>();

        // Sammle alle IDs und finde Duplikate
        for (IdManager.FileItem item : items) {
            String newIdStr = tempNewIds.get(item.getId());
            int newId;

            if (newIdStr != null) {
                newId = Integer.parseInt(newIdStr);
            } else {
                newId = item.getId();
            }

            // Wenn diese ID bereits verwendet wurde, markiere sie als Duplikat
            if (!usedNewIds.add(newId)) {
                duplicateIds.put(item.getId(), newId);
            } else {
                oldToNewIds.put(item.getId(), newId);
            }
        }

        // Zweite Phase: Korrigiere Duplikate
        if (!duplicateIds.isEmpty()) {
            int maxId = findMaxId(oldToNewIds.values());

            // Weise den Duplikaten neue, eindeutige IDs zu
            for (Map.Entry<Integer, Integer> entry : duplicateIds.entrySet()) {
                int oldId = entry.getKey();
                maxId++;
                oldToNewIds.put(oldId, maxId);

                // Zeige dem Benutzer an, dass eine ID geändert wurde
                String message = "Duplicate ID " + entry.getValue() + " was changed to " + maxId;
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }

        if (idChangeListener != null) {
            idChangeListener.onIdsChanged(oldToNewIds);
        }

        isEditMode = false;
        tempNewIds.clear();
        notifyDataSetChanged();
    }

    private int findMaxId(Collection<Integer> ids) {
        int maxId = 0;
        for (Integer id : ids) {
            maxId = Math.max(maxId, id);
        }
        return maxId;
    }
    public void updateItems(List<IdManager.FileItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    public IdManager.FileItem getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    private TextWatcher createTextWatcher(ViewHolder holder, int position) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isUserInput = after > count;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 3 && isUserInput) {
                    try {
                        int newId = Integer.parseInt(s.toString());
                        handleIdChange(position, s.toString());
                        moveToNextItem(position);
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                }
                isUserInput = false;
            }
        };
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        final EditText globalIdView;
        final TextView titleView;
        final TextView authorView;
        private TextWatcher textWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            globalIdView = itemView.findViewById(R.id.globalIdView);
            titleView = itemView.findViewById(R.id.titleView);
            authorView = itemView.findViewById(R.id.authorView);
        }

        public TextWatcher getTextWatcher() {
            return textWatcher;
        }

        public void setTextWatcher(TextWatcher watcher) {
            this.textWatcher = watcher;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
