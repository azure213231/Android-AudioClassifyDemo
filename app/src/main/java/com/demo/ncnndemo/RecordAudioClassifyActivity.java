package com.demo.ncnndemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.demo.ncnndemo.databinding.ActivityRecordAudioClassifyBinding;

public class RecordAudioClassifyActivity extends AppCompatActivity {

    ActivityRecordAudioClassifyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecordAudioClassifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {

    }
}