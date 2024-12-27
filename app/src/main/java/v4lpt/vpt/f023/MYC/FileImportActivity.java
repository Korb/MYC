package v4lpt.vpt.f023.MYC;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileImportActivity extends AppCompatActivity {
    private IdManager idManager;
    private File memorizeDir;
    private List<PoemContent> poemContents;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleSelectedFile(result.getData().getData());
                } else {
                    finish();
                }
            }
    );

    private final ActivityResultLauncher<Intent> idSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    int startId = result.getData().getIntExtra("CHOSEN_START_ID", -1);
                    if (startId != -1) {
                        importPoemsWithStartId(startId);
                    }
                }
                finish();
            }
    );

    static class PoemContent {
        String author;
        String title;
        String year;
        String content;

        PoemContent(String author, String title, String year, String content) {
            this.author = author;
            this.title = title;
            this.year = year;
            this.content = content;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        idManager = new IdManager(this);
        memorizeDir = new File(getExternalFilesDir(null), "Memorizer");
        poemContents = new ArrayList<>();

        openFilePicker();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        String[] mimeTypes = {
                "text/plain",
                "application/octet-stream",
                "text/x-unknown"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        filePickerLauncher.launch(Intent.createChooser(intent, "Select Text File"));
    }

    private void handleSelectedFile(Uri uri) {
        try {
            poemContents.clear();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder currentPoem = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("---")) {
                    if (currentPoem.length() > 0) {
                        processPoem(currentPoem.toString());
                        currentPoem = new StringBuilder();
                    }
                } else {
                    currentPoem.append(line).append("\n");
                }
            }

            if (currentPoem.length() > 0) {
                processPoem(currentPoem.toString());
            }

            if (poemContents.isEmpty()) {
                Toast.makeText(this, "No valid poems found in file", Toast.LENGTH_SHORT).show();
                finish();
            } else if (poemContents.size() == 1) {
                importPoem(poemContents.get(0), idManager.getNextId());
                finish();
            } else {
                showIdSelectionScreen();
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showIdSelectionScreen() {
        int suggestedStartId = ((getHighestExistingId() + 10) / 10) * 10;

        Intent intent = new Intent(this, IdSelectionActivity.class);
        intent.putExtra("POEM_COUNT", poemContents.size());
        intent.putExtra("SUGGESTED_ID", suggestedStartId);
        idSelectionLauncher.launch(intent);
    }

    private void processPoem(String poemText) {
        String[] lines = poemText.trim().split("\n", 4);
        if (lines.length >= 4) {
            String author = lines[0].trim();
            String title = lines[1].trim();
            String year = lines[2].trim();
            String content = lines[3].trim();

            if (!author.isEmpty() && !title.isEmpty() && !year.isEmpty() && !content.isEmpty()) {
                poemContents.add(new PoemContent(author, title, year, content));
            }
        }
    }

    private int getHighestExistingId() {
        List<IdManager.FileItem> allFiles = idManager.getAllFiles(memorizeDir);
        int highestId = 0;
        for (IdManager.FileItem item : allFiles) {
            if (item.getId() > highestId) {
                highestId = item.getId();
            }
        }
        return highestId;
    }

    private void importPoemsWithStartId(int startId) {
        // Get all existing IDs to avoid conflicts
        Set<Integer> existingIds = new HashSet<>();
        List<IdManager.FileItem> allFiles = idManager.getAllFiles(memorizeDir);
        for (IdManager.FileItem item : allFiles) {
            existingIds.add(item.getId());
        }

        // Import poems with sequential IDs, skipping existing ones
        int currentId = startId;
        for (PoemContent poem : poemContents) {
            // Find next available ID
            while (existingIds.contains(currentId)) {
                currentId++;
            }
            importPoem(poem, currentId);
            existingIds.add(currentId); // Add the used ID to the set
            currentId++;
        }
    }

    private void importPoem(PoemContent poem, int id) {
        File categoryDir = new File(memorizeDir, "Poems");
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
        }

        File newFile = new File(categoryDir, id + "_" + System.currentTimeMillis() + ".txt");

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            String fileContent = poem.author + "\n" + poem.title + "\n" + poem.year +
                    "\nstart_content\n" + poem.content;
            out.write(fileContent.getBytes());
        } catch (IOException e) {
            Toast.makeText(this, "Error importing poem", Toast.LENGTH_SHORT).show();
        }
    }
}