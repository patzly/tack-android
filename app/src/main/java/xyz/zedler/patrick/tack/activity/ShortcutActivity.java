package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import xyz.zedler.patrick.tack.service.MetronomeService;

public class ShortcutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MainActivity.class));
        startService(getIntent().setClass(this, MetronomeService.class));
        finish();
    }
}
