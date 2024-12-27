package v4lpt.vpt.f023.MYC;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class IdSelectionActivity extends AppCompatActivity {
    private EditText etStartId;
    private Button btnYes;
    private Button btnNo;
    private TextView tvImportCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_selection);

        int poemCount = getIntent().getIntExtra("POEM_COUNT", 0);
        int suggestedId = getIntent().getIntExtra("SUGGESTED_ID", 0);

        initializeViews();
        setupContent(poemCount, suggestedId);
        setupListeners();
    }

    private void initializeViews() {
        etStartId = findViewById(R.id.etStartId);
        btnYes = findViewById(R.id.btnYes);
        btnNo = findViewById(R.id.btnNo);
        tvImportCount = findViewById(R.id.tvImportCount);
    }

    private void setupContent(int poemCount, int suggestedId) {
        tvImportCount.setText("Import " + poemCount + " poems?");
        etStartId.setText(String.valueOf(suggestedId));
    }

    private void setupListeners() {
        btnYes.setOnClickListener(v -> {
            // Return the chosen ID to FileImportActivity
            int chosenId = Integer.parseInt(etStartId.getText().toString());
            setResult(Activity.RESULT_OK,
                    getIntent().putExtra("CHOSEN_START_ID", chosenId));
            finish();
        });

        btnNo.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }
}