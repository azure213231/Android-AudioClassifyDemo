package com.demo.ncnndemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.demo.ncnndemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'ncnndemo' library on application startup.
    static {
        System.loadLibrary("ncnndemo");
    }

    private Integer SIZE = 40;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.assetsAudioClassifyLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssetsAudioClassifyActivity();
            }
        });

        binding.recordAudioClassifyLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordAudioClassifyActivity();
            }
        });
    }

    private void showAssetsAudioClassifyActivity() {
        Intent intent = new Intent(this, AssetsAudioClassify.class);
        startActivity(intent);
    }

    private void showRecordAudioClassifyActivity() {
        Intent intent = new Intent(this, RecordAudioClassifyActivity.class);
        startActivity(intent);
    }
}