package com.linfeng.music.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.ID3v24Tag;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class Mp3TagUtils {
    /**
     * 写入MP3文件的ID3标签
     * @param mp3Path mp3文件路径
     * @param metaMap 元数据Map，key支持：title, artist, album, albumArtist, genre, composer, coverUrl
     * @throws Exception 失败抛出异常
     */
    public static void writeTags(String mp3Path, Map<String, String> metaMap) throws Exception {
        // 验证文件存在
        File origFile = new File(mp3Path);
        if (!origFile.exists() || !origFile.isFile()) {
            throw new IllegalArgumentException("MP3 file does not exist or is not a file");
        }
        
        // 记录原文件大小，用于验证
        long origFileLength = origFile.length();
        if (origFileLength == 0) {
            throw new IllegalArgumentException("MP3 file is empty");
        }
        
        Mp3File mp3file = null;
        File tempFile = null;
        try {
            // 读取MP3文件
            mp3file = new Mp3File(mp3Path);
            ID3v2 id3v2Tag;
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v2Tag(id3v2Tag);
            }
            
            // 标题
            id3v2Tag.setTitle(metaMap.getOrDefault("title", ""));
            // 艺术家
            id3v2Tag.setArtist(metaMap.getOrDefault("artist", ""));
            // 专辑
            id3v2Tag.setAlbum(metaMap.getOrDefault("album", ""));
            // 专辑艺术家
            id3v2Tag.setAlbumArtist(metaMap.getOrDefault("albumArtist", ""));
            // 参与创作的艺术家
            id3v2Tag.setComposer(metaMap.getOrDefault("composer", ""));
            // 流派
            id3v2Tag.setGenreDescription(metaMap.getOrDefault("genre", ""));
            
            // 封面（失败不影响其他标签写入）
            String coverUrl = metaMap.get("coverUrl");
            if (!TextUtils.isEmpty(coverUrl)) {
                try {
                    byte[] imageData = downloadImage(coverUrl);
                    if (imageData != null && imageData.length > 0) {
                        id3v2Tag.setAlbumImage(imageData, "image/jpeg");
                    }
                } catch (Exception e) {
                    // 封面下载失败不影响其他标签写入
                    e.printStackTrace();
                }
            }
            
            // 保存到临时文件
            String tempPath = mp3Path + ".tagtmp";
            tempFile = new File(tempPath);
            // 确保临时文件不存在
            if (tempFile.exists()) {
                tempFile.delete();
            }
            
            // 保存文件
            mp3file.save(tempPath);
            
            // 验证临时文件是否创建成功且大小合理
            if (!tempFile.exists() || tempFile.length() == 0) {
                throw new IOException("Failed to create temporary file or file is empty");
            }
            
            // 替换原文件
            if (origFile.delete()) {
                boolean renamed = tempFile.renameTo(origFile);
                if (!renamed) {
                    // 如果重命名失败，尝试复制临时文件内容到原文件
                    copyFile(tempFile, origFile);
                    tempFile.delete();
                }
            } else {
                throw new IOException("Failed to delete original file");
            }
        } catch (Exception e) {
            // 如果出现异常，确保原文件不受影响
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }
    
    // 复制文件的辅助方法
    private static void copyFile(File source, File dest) throws IOException {
        java.io.FileInputStream fis = null;
        java.io.FileOutputStream fos = null;
        java.nio.channels.FileChannel inChannel = null;
        java.nio.channels.FileChannel outChannel = null;
        try {
            fis = new java.io.FileInputStream(source);
            fos = new java.io.FileOutputStream(dest);
            inChannel = fis.getChannel();
            outChannel = fos.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }

    // 下载图片为byte[]
    private static byte[] downloadImage(String urlStr) {
        HttpURLConnection conn = null;
        java.io.InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000); // 增加超时时间
            conn.setReadTimeout(10000);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            
            is = conn.getInputStream();
            
            // 直接读取字节流，避免Bitmap处理可能的OOM
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            
            byte[] imageData = baos.toByteArray();
            // 验证图片数据大小
            if (imageData.length < 100) { // 图片数据太小，可能不是有效图片
                return null;
            }
            
            return imageData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (baos != null) baos.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 