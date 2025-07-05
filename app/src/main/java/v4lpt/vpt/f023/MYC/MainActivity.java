package v4lpt.vpt.f023.MYC;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private PinInputView pinInput;
    private File memorizeDir;
    private IdManager idManager;
    private View rootView;
    private View mainLayout, infoLayout;
    private Button infoButton;
    private MaterialButton btnExportDb;
    private MaterialButton btnImportDb;

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
        setContentView(R.layout.activity_main);
        mainLayout = findViewById(R.id.chooseLayout);
        infoLayout = findViewById(R.id.infoLayout);
        showMainLayout();
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> showInfoLayout());
        View backButton23 = infoLayout.findViewById(R.id.backButton23);
        backButton23.setOnClickListener(v -> showMainLayout());

        rootView = findViewById(android.R.id.content);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        pinInput = findViewById(R.id.pinInput);
        Button btnImport = findViewById(R.id.btnImport);
        Button btnPoems = findViewById(R.id.btnPoems);
        MaterialButton btnImportFile = findViewById(R.id.btnImportFile);
        btnExportDb = findViewById(R.id.btnExportDb);
        btnImportDb = findViewById(R.id.btnImportDb);


        memorizeDir = new File(getExternalFilesDir(null), "Memorizer");
        if (!memorizeDir.exists()) {
            memorizeDir.mkdirs();
        }

        idManager = new IdManager(this);

        if (isFirstRun()) {
            importDefaultFiles();
            setFirstRunComplete();
        }

        pinInput.setOnPinCompleteListener(pin -> {
            int id = Integer.parseInt(pin);
            openFileById(id);
            hideKeyboard();
            pinInput.setText("");
            rootView.requestFocus();
        });

        btnImport.setOnClickListener(v -> startActivity(new Intent(this, ImportActivity.class)));
        btnPoems.setOnClickListener(v -> showFilesInCategory("Poems"));
        btnImportFile.setOnClickListener(v -> startActivity(new Intent(this, FileImportActivity.class)));

        btnExportDb.setOnClickListener(v -> handleExportDatabase());
        btnImportDb.setOnClickListener(v -> handleImportDatabase());
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


    private void openFileById(int id) {
        List<IdManager.FileItem> allFiles = idManager.getAllFiles(memorizeDir);
        for (IdManager.FileItem item : allFiles) {
            if (item.getId() == id) {
                Intent intent = new Intent(this, ViewActivity.class);
                intent.putExtra("FILE_PATH", item.getFile().getAbsolutePath());
                intent.putExtra("GLOBAL_ID", id);
                intent.putExtra("CATEGORY", item.getCategory());
                startActivity(intent);
                return;
            }
        }
        Toast.makeText(this, "Poem with ID " + id + " not found.", Toast.LENGTH_SHORT).show();
    }

    private void showFilesInCategory(String category) {
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra("CATEGORY", category);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pinInput.setText("");
        rootView.requestFocus();
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
    }

    private void importDefaultFiles() {
        String[] categories = {"Songs", "Poems", "Music"};

        for (String category : categories) {
            File categoryDir = new File(memorizeDir, category);
            categoryDir.mkdirs();

            int fileIndex = 1;
            while (true) {
                String resourceName;
                if (category.equals("Music")) {
                    resourceName = "default_music" + fileIndex;
                } else {
                    resourceName = "default_" + category.toLowerCase().substring(0, category.length() - 1) + fileIndex;
                }

                int resourceId = getResources().getIdentifier(resourceName, "raw", getPackageName());

                if (resourceId == 0) {
                    break; // No more default files for this category
                }

                try {
                    InputStream inputStream = getResources().openRawResource(resourceId);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    String author = reader.readLine();
                    String title = reader.readLine();
                    String year = reader.readLine();
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }

                    int globalId = idManager.getNextId();
                    File newFile = new File(categoryDir, globalId + "_" + System.currentTimeMillis() + ".txt");
                    try (FileOutputStream out = new FileOutputStream(newFile)) {
                        String fileContent = author + "\n" + title + "\n" + year + "\nstart_content\n" + content.toString();
                        out.write(fileContent.getBytes());
                    }

                    fileIndex++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getBoolean("FirstRun", true);
    }

    private void setFirstRunComplete() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("FirstRun", false).apply();
    }
    private void showMainLayout() {
        mainLayout.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.GONE);
    }
    private void showInfoLayout() {
        mainLayout.setVisibility(View.GONE);
        infoLayout.setVisibility(View.VISIBLE);
    }
}