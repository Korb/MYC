package v4lpt.vpt.f023.MYC;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private PinInputView pinInput;
    private File memorizeDir;
    private IdManager idManager;
    private View rootView;
    private View mainLayout, infoLayout;
    private Button infoButton;
    private MaterialButton btnBackupRestore;


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
        btnBackupRestore = findViewById(R.id.btnBackupRestore);


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

        btnBackupRestore.setOnClickListener(v -> startActivity(new Intent(this, BackupActivity.class)));
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