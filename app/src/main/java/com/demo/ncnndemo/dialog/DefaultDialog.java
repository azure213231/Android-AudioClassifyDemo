package com.demo.ncnndemo.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;

import com.demo.ncnndemo.databinding.DialogDefaultBinding;

public class DefaultDialog extends Dialog {

    DialogDefaultBinding binding;

    private Context context;
    private onClickListener listener;
    private String title;
    private String content;

    public DefaultDialog(@NonNull Context context, String title, String content, onClickListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.title = title;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogDefaultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setCancelable(true);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initView();
    }

    private void initView() {
        binding.dialogDefaultBackgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        binding.dialogDefaultLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        if (this.title != null){
            binding.dialogDefaultTitle.setText(this.title);
        } else {
            binding.dialogDefaultTitle.setVisibility(View.GONE);
        }

        if (this.content != null){
            binding.dialogDefaultContent.setText(this.content);
        } else {
            binding.dialogDefaultContent.setVisibility(View.GONE);
        }

        binding.dialogDefaultConfirmLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null){
                    listener.onConfirmCLick();
                    dismiss();
                }
            }
        });

        binding.dialogDefaultCancelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null){
                    listener.onCancelCLick();
                    dismiss();
                }
            }
        });
    }

    public abstract static class onClickListener{
        public void onConfirmCLick(){};
        public void onCancelCLick(){};
    }
}
