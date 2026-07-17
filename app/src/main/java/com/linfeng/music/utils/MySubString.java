package com.linfeng.music.utils;

import android.util.Log;

/**
 * 字符串处理工具类
 * 所有处理的字符串返回值中都不包括条件字段
 *
 * @author zhaosir
 */

public class MySubString {
    /**
     * 从给定字符串中截取从指定开始标记之后到字符串末尾的内容。
     *
     * @param input 输入字符串
     * @param start 开始标记
     * @return 截取的内容，如果找不到开始标记则返回 null
     */
    public static String subStartString(String input, String start) {
        if (input == null || start == null || input.isEmpty() || start.isEmpty()) {
            return null;
        }

        int startIndex = input.indexOf(start);
        if (startIndex == -1) {
            Log.e("MySubString", "未找到开始标记!");
            return null; // 或者抛出异常
        }

        // 计算开始标记之后的第一个字符的位置
        int contentStartIndex = startIndex + start.length();
        if (contentStartIndex > input.length()) {
            Log.e("MySubString", "开始标记位于字符串末尾之后!");
            return null;
        }

        // 返回不包含开始标记的子串
        return input.substring(contentStartIndex);
    }

    /**
     * 从给定字符串中截取从开头到指定结束标记之间的内容。
     *
     * @param input 输入字符串
     * @param end   结束标记
     * @return 截取的内容，如果找不到结束标记则返回 null
     */
    public static String subEndString(String input, String end) {
        if (input == null || end == null || input.isEmpty() || end.isEmpty()) {
            return null;
        }

        int endIndex = input.indexOf(end);
        if (endIndex == -1) {
            Log.e("MySubString", "未找到结束标记");
            return null; // 或者抛出异常
        }

        return input.substring(0, endIndex);
    }

    /**
     * 从给定字符串中截取指定起始标记和结束标记之间的内容。
     * 注意：不包含起始标记和结束标记，且不用担心结束标记问题。
     *
     * @param input 输入字符串
     * @param start 起始标记
     * @param end   结束标记
     * @return 截取的内容，如果找不到标记则返回 null
     */
    public static String subString(String input, String start, String end) {
        if (input == null || start == null || end == null || input.isEmpty() || start.isEmpty() || end.isEmpty()) {
            return null;
        }

        int startIndex = input.indexOf(start);
        if (startIndex == -1) {
            Log.e("MySubString", "未找到起始标记");
            return null; // 或者抛出异常
        }
        startIndex += start.length(); // 移动到起始标记之后

        // 从起始标记之后字符串搜索结束标记
        int endIndex = input.indexOf(end, startIndex);
        if (endIndex == -1) {
            Log.e("MySubString", "未找到结束标记");
            return null; // 或者抛出异常
        }

        return input.substring(startIndex, endIndex);
    }
}
