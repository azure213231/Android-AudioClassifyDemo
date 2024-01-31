package com.demo.ncnndemo;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static Handler mHandler;
    private static ExecutorService EXECUTORS_INSTANCE;
    private static ExecutorService FILES_DOWN_OR_UPLOAD_EXECUTORS_INSTANCE;
    private static ExecutorService SINGLE_DB_EXECUTORS_INSTANCE;

    private static Executor getExecutor() {
        if (EXECUTORS_INSTANCE == null) {
            synchronized (ThreadPool.class) {
                if (EXECUTORS_INSTANCE == null) {
                    EXECUTORS_INSTANCE = Executors.newFixedThreadPool(6);
                }
            }
        }

        return EXECUTORS_INSTANCE;
    }

    private static Executor getFileDownloadOrUploadExecutor() {
        if (FILES_DOWN_OR_UPLOAD_EXECUTORS_INSTANCE == null) {
            synchronized (ThreadPool.class) {
                if (FILES_DOWN_OR_UPLOAD_EXECUTORS_INSTANCE == null) {
                    FILES_DOWN_OR_UPLOAD_EXECUTORS_INSTANCE = Executors.newFixedThreadPool(3);
                }
            }
        }

        return FILES_DOWN_OR_UPLOAD_EXECUTORS_INSTANCE;
    }

    private static Executor getSingleDBExecutor() {
        if (SINGLE_DB_EXECUTORS_INSTANCE == null) {
            synchronized (ThreadPool.class) {
                if (SINGLE_DB_EXECUTORS_INSTANCE == null) {
                    SINGLE_DB_EXECUTORS_INSTANCE = Executors.newSingleThreadExecutor();
                }
            }
        }

        return SINGLE_DB_EXECUTORS_INSTANCE;
    }

    public static void runOnMainThread(Runnable runnable) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        mHandler.post(runnable);
    }

    public static void runOnThread(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    public static void runOnFileDownloadOrUploadThread(Runnable runnable) {
        getFileDownloadOrUploadExecutor().execute(runnable);
    }

    public static void runOnSingleDBThread(Runnable runnable) {
        getSingleDBExecutor().execute(runnable);
    }
}
