package com.example.k7ebookificate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TextEditActivity extends AppCompatActivity {
    private static final long TEXT_MAX = 16 * 1048576;

    private EditText editFileName;
    private EditText editBody;

    private final ActivityResultLauncher<Intent> pickLaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    List<IO1.VFile> files = IO1.HandleSelectedFile(r.getData());
                    if (!files.isEmpty())
                        loadFile(files.get(0));
                }
            });

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.view_textedit);
        editFileName = findViewById(R.id.editFileName);
        editBody = findViewById(R.id.editBody);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); // Back
        findViewById(R.id.btnLoad).setOnClickListener(v -> IO1.SelectFile(pickLaunch, false)); // Load
        findViewById(R.id.btnSave).setOnClickListener(v -> saveFile()); // Save
    }

    // Read a text file into the editor.
    private void loadFile(IO1.VFile vf) {
        try {
            // Check size limit
            long size = vf.GetSize(this);
            if (size > TEXT_MAX) {
                Toast.makeText(this,
                        "ERR open: file too large", Toast.LENGTH_SHORT).show();
                return;
            }

            // Read content
            byte[] buf;
            try (InputStream in = vf.OpenReader(this)) {
                buf = in.readAllBytes();
            }

            // Apply to UI
            String name = vf.GetName(this);
            if (name != null && !name.isEmpty())
                editFileName.setText(name);
            editBody.setText(new String(buf, StandardCharsets.UTF_8));

        } catch (Exception e) {
            Toast.makeText(this, "ERR open: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Save the editor content to the Downloads.
    private void saveFile() {
        String name = editFileName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a filename", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = editBody.getText().toString();
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        if (data.length > TEXT_MAX) {
            Toast.makeText(this, "ERR save: file too large", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            IO1.VFile outFile = IO1.CreateDownloadsFile(this, name);
            if (outFile == null) {
                Toast.makeText(this, "ERR save: cannot create file", Toast.LENGTH_SHORT).show();
                return;
            }
            try (OutputStream out = outFile.OpenWriter(this, false)) {
                out.write(data);
            }
            Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ERR save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
