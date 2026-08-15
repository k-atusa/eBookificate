package com.example.k7ebookificate;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int[] CARD_ID = {
            R.id.cardStorage0, R.id.cardStorage1, R.id.cardStorage2,
            R.id.cardStorage3, R.id.cardStorage4
    };
    private static final int[] TEXT_ID = {
            R.id.txtStorage0, R.id.txtStorage1, R.id.txtStorage2,
            R.id.txtStorage3, R.id.txtStorage4
    };
    private static final int[] EDIT_ID = {
            R.id.btnEdit0, R.id.btnEdit1, R.id.btnEdit2,
            R.id.btnEdit3, R.id.btnEdit4
    };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.view_main);

        // Init storage directories
        for (int i = 0; i < 5; i++) Core.getStoreDir(this, i);

        // Convert button
        findViewById(R.id.btnConvert).setOnClickListener(v ->
                startActivity(new Intent(this, ConvertActivity.class)));

        // Storage card click and edit button
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            findViewById(CARD_ID[i]).setOnClickListener(v -> {
                Intent it = new Intent(this, StorageActivity.class);
                it.putExtra("slot", slot);
                startActivity(it);
            });
            findViewById(EDIT_ID[i]).setOnClickListener(v -> showRename(slot));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNames();
    }

    // Refresh storage name labels.
    private void loadNames() {
        String[] names = Core.loadNames(this);
        for (int i = 0; i < 5; i++)
            ((TextView) findViewById(TEXT_ID[i])).setText(names[i]);
    }

    // Show rename dialog for a storage slot.
    private void showRename(int slot) {
        String[] names = Core.loadNames(this);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(names[slot]);
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        Core.saveName(this, slot, val);
                        loadNames();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
