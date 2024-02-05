package com.demo.ncnndemo.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.ncnndemo.AppFileActivity;
import com.demo.ncnndemo.R;

import java.io.File;
import java.util.List;

public class FolderAdapter  extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<File> folderList;
    private OnItemClickListener onItemClickListener;

    public FolderAdapter(List<File> folderList, OnItemClickListener onItemClickListener) {
        this.folderList = folderList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        File folder = folderList.get(position);
        holder.folderName.setText(folder.getName());

        // 设置点击事件
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                if (folder.isDirectory() && onItemClickListener != null) {
                    onItemClickListener.onFolderClick(folder);
                } else if (!folder.isDirectory() && onItemClickListener != null) {
                    onItemClickListener.onFileClick(folder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderName);
        }
    }

    // 接口用于处理点击事件
    public interface OnItemClickListener {
        void onFolderClick(File folder);
        void onFileClick(File file);
    }
}
