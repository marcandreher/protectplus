package io.osuprotectplus.crawler;

import java.io.File;

import com.moandjiezana.toml.Toml;

/**
 * Configuration class for the ProtectPlus Crawler
 */
public class Config {
    public String osuApiKey;
    
    public static Config load(File file) {
        Toml toml = new Toml().read(file);
        Config config = new Config();
        config.osuApiKey = toml.getString("osuapi.key");
        return config;
    }
    
    public boolean isValid() {
        return osuApiKey != null && !osuApiKey.isEmpty();
    }
}