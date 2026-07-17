package com.linfeng.music.utils;

import java.util.Map;
import java.util.Objects;

public class ConfigsRepository {
    private static Map<String, Object> configs;

    public static Map<String, Object> getConfigs() {
        return configs;
    }

    public static void setConfigs(Map<String, Object> configs) {
        ConfigsRepository.configs = configs;
    }
}
