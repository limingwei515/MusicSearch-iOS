package com.linfeng.music.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.linfeng.music.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CommonUtils {
    private static final String TAG = "MusicNotify";

    /**
     * AES解密（BASE64）
     *
     * @param encryptedData 需要解密的文档
     * @param key           解密密钥
     * @return 解密后的明文
     * @throws Exception 抛出异常
     */

    public static String decryptAES(String encryptedData, String key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] iv = new byte[16];
        System.arraycopy(decoded, 0, iv, 0, 16);
        byte[] encryptedBytes = new byte[decoded.length - 16];
        System.arraycopy(decoded, 16, encryptedBytes, 0, decoded.length - 16);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 格式化数字字符串
     *
     * @param numberStr 输入的数字字符串
     * @return 格式化后的字符串
     */
    public static String formatNumber(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return "0";
        }
        try {
            int number = Integer.parseInt(numberStr.trim());
            if (numberStr.length() == 4) {
                return numberStr;
            } else if (numberStr.length() >= 5) {
                double formattedNumber = number / 10000.0;
                // 使用DecimalFormat实现向下取整
                DecimalFormat df = new DecimalFormat("#.0");
                df.setRoundingMode(RoundingMode.DOWN);
                return df.format(formattedNumber) + "万";
            }
            return numberStr;
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    /**
     * 从 assets 目录中读取指定文件的内容。
     *
     * @param context  上下文对象
     * @param fileName 文件名（包括路径，如 "config/settings.json"）
     * @return 文件内容（字符串），如果文件不存在或发生错误则返回 null
     */
    public static String getFileContentFromAssets(Context context, String fileName) {
        if (context == null || fileName == null || fileName.isEmpty()) {
            Log.e(TAG, "Context or fileName is null or empty");
            return null;
        }

        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            // 打开 assets 中的文件流
            InputStream inputStream = context.getAssets().open(fileName);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // 逐行读取文件内容
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            // 返回文件内容
            return content.toString().trim(); // 去除末尾多余的换行符
        } catch (IOException e) {
            Log.e(TAG, "没有找到该文件: " + fileName, e);
            return null;
        } finally {
            // 关闭流
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close BufferedReader", e);
                }
            }
        }
    }

    /**
     * 将秒数转换为分:秒格式（如：10 → 0:10，60 → 1:00）
     *
     * @param seconds 需要转换的秒数（非负整数）
     * @return 格式化后的时间字符串
     * @throws IllegalArgumentException 当输入负数时抛出异常
     */
    public static String formatTime(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("秒数不能为负数");
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    /**
     * 进入应用检查是否存在指定目录
     * 如果存在则不执行操作
     * 如果不存在则创建指定目录
     *
     * @param context  上下文对象，用于获取应用名称
     * @param pathName 目录名
     */
    public static void createDirs(Context context, String pathName) {
        // 获取外部存储目录并构建目标路径
        String appName = context.getString(R.string.app_name);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + appName + "/" + pathName;

        // 创建文件对象
        File file = new File(path);
        // 检查目录是否存在
        if (!file.exists()) {
            // 尝试创建目录
            boolean isCreated = file.mkdirs(); // 创建多级目录
            if (isCreated) {
                Log.d(TAG, String.format("【提示】创建目录%s成功！", path));
            } else {
                Log.e(TAG, String.format("【警告】创建目录%s失败！", path));
            }
        } else {
            Log.d(TAG, String.format("【提示】目录%s已存在！", path));
        }
    }
    /**
     * 获取版本号
     *
     * @param context 上下文对象
     * @return 版本号
     */
    public static String getVersionName(Context context, String defaultValue) {
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "【错误】获取当前版本失败，无法判断更新！", e);
            return defaultValue;
        }
    }


}
