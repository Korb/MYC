package v4lpt.vpt.f023.MYC;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class BackupActivity extends AppCompatActivity {

    private IdManager idManager;
    private File memorizeDir;

    // Constants for backup file structure
    private static final String DB_ENTRY_SEPARATOR = "---DB_ENTRY_SEPARATOR---";
    private static final String CONTENT_START_MARKER = "---CONTENT_START---";
    private static final String CONTENT_END_MARKER = "---CONTENT_END---";
    private static final String CATEGORY_MARKER = "CATEGORY:";
    private static final String FILENAME_MARKER = "FILENAME:";

    // Launcher for exporting DB
    private final ActivityResultLauncher<Intent> exportDbLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performDbExport(uri);
                    } else {
                        Toast.makeText(this, "Export cancelled: Could not get file path.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Launcher for importing DB
    private final ActivityResultLauncher<Intent> importDbLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performDbImport(uri);
                    } else {
                        Toast.makeText(this, "Import cancelled: Could not get file.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        idManager = new IdManager(this);
        memorizeDir = new File(getExternalFilesDir(null), "Memorizer");

        MaterialButton btnExport = findViewById(R.id.btnExport);
        MaterialButton btnImport = findViewById(R.id.btnImport);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        btnExport.setOnClickListener(v -> handleExportDatabase());
        btnImport.setOnClickListener(v -> handleImportDatabase());
        btnBack.setOnClickListener(v -> finish());
    }

    private void handleExportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "memorizer_backup_" + System.currentTimeMillis() + ".txt");
        exportDbLauncher.launch(intent);
    }

    private void performDbExport(Uri uri) {
        List<IdManager.FileItem> allFiles = idManager.getAllFiles(memorizeDir);
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            for (IdManager.FileItem item : allFiles) {
                writer.write(CATEGORY_MARKER + item.getCategory());
                writer.newLine();
                writer.write(FILENAME_MARKER + item.getFile().getName());
                writer.newLine();
                writer.write(CONTENT_START_MARKER);
                writer.newLine();

                // Write file content
                try (BufferedReader fileReader = new BufferedReader(new FileReader(item.getFile()))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                }

                writer.write(CONTENT_END_MARKER);
                writer.newLine();
                writer.write(DB_ENTRY_SEPARATOR);
                writer.newLine();
            }
            Toast.makeText(this, "Database exported successfully.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleImportDatabase() {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Import Database")
                .setMessage("This replaces all your previous poems, you sure?")
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/plain");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    importDbLauncher.launch(Intent.createChooser(intent, "Select Backup File"));
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void performDbImport(Uri uri) {
        // 1. Clear the memorizer directory
        clearMemorizerDirectory();

        int highestId = 0;

        // 2. Read backup and restore
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            String currentCategory = null;
            String currentFilename = null;
            StringBuilder contentBuilder = new StringBuilder();
            boolean inContentBlock = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(CATEGORY_MARKER)) {
                    currentCategory = line.substring(CATEGORY_MARKER.length());
                } else if (line.startsWith(FILENAME_MARKER)) {
                    currentFilename = line.substring(FILENAME_MARKER.length());
                } else if (line.equals(CONTENT_START_MARKER)) {
                    contentBuilder = new StringBuilder();
                    inContentBlock = true;
                } else if (line.equals(CONTENT_END_MARKER)) {
                    inContentBlock = false;
                } else if (line.equals(DB_ENTRY_SEPARATOR)) {
                    // Save the completed entry
                    if (currentCategory != null && currentFilename != null && contentBuilder.length() > 0) {
                        saveImportedPoem(currentCategory, currentFilename, contentBuilder.toString());
                        try {
                            int id = Integer.parseInt(currentFilename.split("_")[0]);
                            if (id > highestId) {
                                highestId = id;
                            }
                        } catch (Exception e) {
                            // Ignore if filename is malformed
                        }
                    }
                    // Reset for next entry
                    currentCategory = null;
                    currentFilename = null;
                } else if (inContentBlock) {
                    contentBuilder.append(line).append("\n");
                }
            }
            Toast.makeText(this, "Database imported successfully.", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error importing database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // 3. Update IdManager to prevent ID conflicts
        idManager.setLastId(highestId);
    }

    private void saveImportedPoem(String category, String filename, String content) {
        File categoryDir = new File(memorizeDir, category);
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
        }

        File newFile = new File(categoryDir, filename);

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            out.write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error writing imported file: " + filename, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearMemorizerDirectory() {
        if (memorizeDir != null && memorizeDir.exists() && memorizeDir.isDirectory()) {
            File[] categories = memorizeDir.listFiles();
            if (categories != null) {
                for (File categoryDir : categories) {
                    deleteRecursively(categoryDir);
                }
            }
        }
    }

    private void deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}