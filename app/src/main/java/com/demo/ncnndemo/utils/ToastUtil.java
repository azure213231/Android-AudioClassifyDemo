package com.demo.ncnndemo.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

public class ToastUtil extends Toast{
    private static String oldMsg;
    protected static Toast toast = null;
    private static long oneTime = 0;
    private static long twoTime = 0;

    /**
     * Construct an empty Toast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */
    public ToastUtil(Context context) {
        super(context);
    }

    public static void showToast(Context context, int resId) {
        showToast(context, context.getString(resId));
    }

    public static void showToast(Context context, int resId, int gravity) {
        showToast(context, context.getString(resId), gravity, 0, 0);
    }

    public static void showToast(Context context, String s, int gravity) {
        showToast(context, s, gravity, 0, 0);
    }

    public static void showToast(Context context, int resId, int gravity, int offX, int offY) {
        showToast(context, context.getString(resId), gravity, offX, offY);
    }

    public static void showToast(Context context, String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }

        // 如果没有Toast实例，创建一个新的
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            oneTime = System.currentTimeMillis();
        } else {
            long twoTime = System.currentTimeMillis();
            // 如果是相同的消息
            if (message.equals(oldMsg)) {
                // 只有在间隔大于一定时间时才显示
                if (twoTime - oneTime > Toast.LENGTH_SHORT) {
                    toast.show();
                }
            } else {
                // 更新消息
                oldMsg = message;
                // 使用新的消息创建Toast
                toast.setText(message);  // 直接设置文本而不重新创建Toast
                toast.show();
            }
        }
        oneTime = System.currentTimeMillis();
    }


    public static void showToast(Context context, String s, int gravity, int offX, int offY) {
        if (toast == null) {
            toast = Toast.makeText(context,s,Toast.LENGTH_SHORT);
            toast.setGravity(gravity, offX, offY);
            toast.show();
            oneTime = System.currentTimeMillis();
        } else {
            twoTime = System.currentTimeMillis();
            if (s.equals(oldMsg)) {
                if (twoTime - oneTime > Toast.LENGTH_SHORT) {
                    toast.show();
                }
            } else {
                oldMsg = s;
//                toast.setText(s);
//                toast.show();
                toast.cancel();
                toast = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                toast.setGravity(gravity, offX, offY);
                toast.show();
            }
        }
        oneTime = twoTime;
    }
}