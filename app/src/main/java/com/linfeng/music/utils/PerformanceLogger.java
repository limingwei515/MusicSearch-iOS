package com.linfeng.music.utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PerformanceLogger {
    
    private static PerformanceLogger instance;
    private BufferedWriter writer;
    private SimpleDateFormat dateFormat;
    private String currentLogFile;
    
    private PerformanceLogger() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }
    
    public static PerformanceLogger getInstance() {
        if (instance == null) {
            synchronized (PerformanceLogger.class) {
                if (instance == null) {
                    instance = new PerformanceLogger();
                }
            }
        }
        return instance;
    }
    
    public void initLogFile(Context context) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "music_perf_" + timestamp + ".txt";
            
            File dir = new File(context.getExternalFilesDir(null), "logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            currentLogFile = new File(dir, fileName).getAbsolutePath();
            writer = new BufferedWriter(new FileWriter(currentLogFile, true));
            
            writeLog("========================================");
            writeLog("性能日志开始 - " + dateFormat.format(new Date()));
            writeLog("========================================");
            writeLog("");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void writeLog(String message) {
        if (writer != null) {
            try {
                writer.write(dateFormat.format(new Date()) + " " + message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void writeLog(String tag, String message) {
        writeLog("[" + tag + "] " + message);
    }
    
    public void writeLogWithTimestamp(String tag, String message, long elapsedMs) {
        writeLog("[" + tag + "] " + message + " [耗时: " + elapsedMs + "ms]");
    }
    
    public void closeLogFile() {
        if (writer != null) {
            try {
                writeLog("");
                writeLog("========================================");
                writeLog("性能日志结束 - " + dateFormat.format(new Date()));
                writeLog("========================================");
                writer.close();
                writer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getCurrentLogFilePath() {
        return currentLogFile;
    }
    
    public File getLogDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
