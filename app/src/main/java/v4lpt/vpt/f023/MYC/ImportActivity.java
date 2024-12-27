package v4lpt.vpt.f023.MYC;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ImportActivity extends AppCompatActivity {

    private EditText etAuthor, etTitle, etYear, etContent;
    private RadioGroup rgCategory;
    private Button btnSave;
    private IdManager idManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        etAuthor = findViewById(R.id.etAuthor);
        etTitle = findViewById(R.id.etTitle);
        etYear = findViewById(R.id.etYear);
        etContent = findViewById(R.id.etContent);
        rgCategory = findViewById(R.id.rgCategory);
        btnSave = findViewById(R.id.btnSave);

        idManager = new IdManager(this);

        btnSave.setOnClickListener(v -> saveEntry());
        String selectedCategory = getIntent().getStringExtra("SELECTED_CATEGORY");
        if (selectedCategory != null) {
            // Set the appropriate radio button based on the category
            switch (selectedCategory) {
                case "Songs":
                    rgCategory.check(R.id.rbSongs);
                    break;
                case "Poems":
                    rgCategory.check(R.id.rbPoems);
                    break;
                case "Music":
                    rgCategory.check(R.id.rbMusic);
                    break;
            }
        }

    }

    private void saveEntry() {
        String author = etAuthor.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String year = etYear.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (author.isEmpty() || title.isEmpty() || year.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String category;
        int selectedId = rgCategory.getCheckedRadioButtonId();
        if (selectedId == R.id.rbSongs) {
            category = "Songs";
        } else if (selectedId == R.id.rbPoems) {
            category = "Poems";
        } else if (selectedId == R.id.rbMusic) {
            category = "Music";
        } else {
            category = "Poems";
            //Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
           //return;
        }

        File memorizeDir = new File(getExternalFilesDir(null), "Memorizer");
        File categoryDir = new File(memorizeDir, category);
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
        }

        int newId = idManager.getNextId();
        File newFile = new File(categoryDir, newId + "_" + System.currentTimeMillis() + ".txt");

        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(author + "\n" + title + "\n" + year + "\nstart_content\n" + content);
            Toast.makeText(this, "Entry saved successfully with ID: " + newId, Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving entry", Toast.LENGTH_SHORT).show();
        }
    }
}