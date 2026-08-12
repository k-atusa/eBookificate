package com.example.k7ebookificate;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private static final int[] CARD_IDS = {
            R.id.cardStorage0, R.id.cardStorage1, R.id.cardStorage2,
            R.id.cardStorage3, R.id.cardStorage4
    };
    private static final int[] TEXT_IDS = {
            R.id.txtStorage0, R.id.txtStorage1, R.id.txtStorage2,
            R.id.txtStorage3, R.id.txtStorage4
    };
    private static final int[] EDIT_IDS = {
            R.id.btnEdit0, R.id.btnEdit1, R.id.btnEdit2,
            R.id.btnEdit3, R.id.btnEdit4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_main);

        // Ensure storage directories exist
        for (int i = 0; i < 5; i++) {
            Core.getStorageDir(this, i);
        }

        // Convert button
        findViewById(R.id.btnConvert).setOnClickListener(v -> {
            startActivity(new Intent(this, ConvertActivity.class));
        });

        // Storage cards
        for (int i = 0; i < 5; i++) {
            final int slot = i;

            // Card click → open StorageActivity
            findViewById(CARD_IDS[i]).setOnClickListener(v -> {
                Intent intent = new Intent(this, StorageActivity.class);
                intent.putExtra("slot", slot);
                startActivity(intent);
            });

            // Edit button → rename dialog
            findViewById(EDIT_IDS[i]).setOnClickListener(v -> showRenameDialog(slot));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNames();
    }

    private void refreshNames() {
        String[] names = Core.loadStorageNames(this);
        for (int i = 0; i < 5; i++) {
            ((TextView) findViewById(TEXT_IDS[i])).setText(names[i]);
        }
    }

    private void showRenameDialog(int slot) {
        String[] names = Core.loadStorageNames(this);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(names[slot]);
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("이름 변경")
                .setView(input)
                .setPositiveButton("저장", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        Core.saveStorageName(this, slot, newName);
                        refreshNames();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
