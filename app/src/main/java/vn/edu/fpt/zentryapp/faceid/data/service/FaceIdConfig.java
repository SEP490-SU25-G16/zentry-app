package vn.edu.fpt.zentryapp.faceid.data.service;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 🔧 NEW: Centralized configuration management for Face ID processing
 * Supports different scenarios and configurable thresholds
 */
public class FaceIdConfig {
    private static final String TAG = "FaceIdConfig";
    private static final String PREFS_NAME = "face_id_config";
    
    // Configuration scenarios
    public enum Scenario {
        REGISTRATION,    // Face registration - more lenient
        VERIFICATION,    // Face verification - balanced
        UPDATE,         // Face update - similar to registration
        SECURITY_CHECK  // High security check - strict
    }
    
    // Memory Management Configuration
    public static class MemoryConfig {
        public final int bitmapPoolSize;
        public final int rectPoolSize;
        public final long maxMemoryUsageBytes;
        public final boolean enableMemoryMonitoring;
        
        public MemoryConfig(int bitmapPoolSize, int rectPoolSize, long maxMemoryUsageBytes, boolean enableMemoryMonitoring) {
            this.bitmapPoolSize = bitmapPoolSize;
            this.rectPoolSize = rectPoolSize;
            this.maxMemoryUsageBytes = maxMemoryUsageBytes;
            this.enableMemoryMonitoring = enableMemoryMonitoring;
        }
        
        public static MemoryConfig getDefault() {
            return new MemoryConfig(10, 20, 50 * 1024 * 1024, true); // 50MB limit
        }
    }
    
    // Performance Configuration
    public static class PerformanceConfig {
        public final int cacheSize;
        public final long cacheExpiryMs;
        public final boolean enableBatchProcessing;
        public final int batchSize;
        public final boolean enableParallelProcessing;
        
        public PerformanceConfig(int cacheSize, long cacheExpiryMs, boolean enableBatchProcessing, 
                               int batchSize, boolean enableParallelProcessing) {
            this.cacheSize = cacheSize;
            this.cacheExpiryMs = cacheExpiryMs;
            this.enableBatchProcessing = enableBatchProcessing;
            this.batchSize = batchSize;
            this.enableParallelProcessing = enableParallelProcessing;
        }
        
        public static PerformanceConfig getDefault() {
            return new PerformanceConfig(50, 30000, false, 5, false); // 30s cache expiry
        }
    }
    
    // Anti-Spoof Configuration
    public static class AntiSpoofConfig {
        public final float highConfidenceThreshold;
        public final float mediumConfidenceThreshold;
        public final float lowConfidenceThreshold;
        public final float veryLowConfidenceThreshold;
        public final int minRealFaceFrames;
        public final int maxSpoofStreak;
        public final float naturalMovementThreshold;
        public final float realFaceRecoveryThreshold;
        
        public AntiSpoofConfig(float highConfidenceThreshold, float mediumConfidenceThreshold,
                             float lowConfidenceThreshold, float veryLowConfidenceThreshold,
                             int minRealFaceFrames, int maxSpoofStreak,
                             float naturalMovementThreshold, float realFaceRecoveryThreshold) {
            this.highConfidenceThreshold = highConfidenceThreshold;
            this.mediumConfidenceThreshold = mediumConfidenceThreshold;
            this.lowConfidenceThreshold = lowConfidenceThreshold;
            this.veryLowConfidenceThreshold = veryLowConfidenceThreshold;
            this.minRealFaceFrames = minRealFaceFrames;
            this.maxSpoofStreak = maxSpoofStreak;
            this.naturalMovementThreshold = naturalMovementThreshold;
            this.realFaceRecoveryThreshold = realFaceRecoveryThreshold;
        }
        
        public static AntiSpoofConfig getDefault() {
            return new AntiSpoofConfig(0.85f, 0.70f, 0.55f, 0.40f, 3, 5, 0.15f, 0.60f);
        }
        
        public static AntiSpoofConfig forScenario(Scenario scenario) {
            switch (scenario) {
                case REGISTRATION:
                    return new AntiSpoofConfig(0.80f, 0.65f, 0.50f, 0.35f, 2, 3, 0.12f, 0.55f);
                case VERIFICATION:
                    return new AntiSpoofConfig(0.85f, 0.70f, 0.55f, 0.40f, 3, 5, 0.15f, 0.60f);
                case UPDATE:
                    return new AntiSpoofConfig(0.80f, 0.65f, 0.50f, 0.35f, 2, 3, 0.12f, 0.55f);
                case SECURITY_CHECK:
                    return new AntiSpoofConfig(0.90f, 0.75f, 0.60f, 0.45f, 5, 2, 0.20f, 0.70f);
                default:
                    return getDefault();
            }
        }
    }
    
    // Oval Boundary Configuration
    public static class OvalConfig {
        public final float minFaceSizeRatio;
        public final float maxFaceSizeRatio;
        public final boolean strictPositioning;
        
        public OvalConfig(float minFaceSizeRatio, float maxFaceSizeRatio, 
                         boolean strictPositioning) {
            this.minFaceSizeRatio = minFaceSizeRatio;
            this.maxFaceSizeRatio = maxFaceSizeRatio;
            this.strictPositioning = strictPositioning;
        }
        
        public static OvalConfig getDefault() {
            return new OvalConfig(0.3f, 0.8f, true);
        }
        
        public static OvalConfig forScenario(Scenario scenario) {
            switch (scenario) {
                case REGISTRATION:
                    return new OvalConfig(0.20f, 0.90f, false); // More lenient for registration
                case VERIFICATION:
                    return new OvalConfig(0.30f, 0.80f, true);
                case UPDATE:
                    return new OvalConfig(0.25f, 0.85f, false);
                case SECURITY_CHECK:
                    return new OvalConfig(0.35f, 0.75f, true);
                default:
                    return getDefault();
            }
        }
    }
    
    // Main Configuration Class
    public static class Config {
        public final MemoryConfig memoryConfig;
        public final PerformanceConfig performanceConfig;
        public final AntiSpoofConfig antiSpoofConfig;
        public final OvalConfig ovalConfig;
        public final Scenario scenario;
        
        public Config(MemoryConfig memoryConfig, PerformanceConfig performanceConfig,
                     AntiSpoofConfig antiSpoofConfig, OvalConfig ovalConfig, Scenario scenario) {
            this.memoryConfig = memoryConfig;
            this.performanceConfig = performanceConfig;
            this.antiSpoofConfig = antiSpoofConfig;
            this.ovalConfig = ovalConfig;
            this.scenario = scenario;
        }
        
        public static Config getDefault() {
            return new Config(
                MemoryConfig.getDefault(),
                PerformanceConfig.getDefault(),
                AntiSpoofConfig.getDefault(),
                OvalConfig.getDefault(),
                Scenario.VERIFICATION
            );
        }
        
        public static Config forScenario(Scenario scenario) {
            return new Config(
                MemoryConfig.getDefault(),
                PerformanceConfig.getDefault(),
                AntiSpoofConfig.forScenario(scenario),
                OvalConfig.forScenario(scenario),
                scenario
            );
        }
    }
    
    // Configuration Manager
    private final Context context;
    private final SharedPreferences prefs;
    private Config currentConfig;
    
    public FaceIdConfig(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.currentConfig = loadConfig();
    }
    
    public Config getConfig() {
        return currentConfig;
    }
    
    public void setScenario(Scenario scenario) {
        this.currentConfig = Config.forScenario(scenario);
        saveConfig();
    }
    
    public void updateConfig(Config config) {
        this.currentConfig = config;
        saveConfig();
    }
    
    private Config loadConfig() {
        String scenarioName = prefs.getString("scenario", Scenario.VERIFICATION.name());
        Scenario scenario = Scenario.valueOf(scenarioName);
        return Config.forScenario(scenario);
    }
    
    private void saveConfig() {
        prefs.edit()
            .putString("scenario", currentConfig.scenario.name())
            .apply();
    }
    
    // Utility methods for easy access to common configs
    public static Config getRegistrationConfig() {
        return Config.forScenario(Scenario.REGISTRATION);
    }
    
    public static Config getVerificationConfig() {
        return Config.forScenario(Scenario.VERIFICATION);
    }
    
    public static Config getUpdateConfig() {
        return Config.forScenario(Scenario.UPDATE);
    }
    
    public static Config getSecurityCheckConfig() {
        return Config.forScenario(Scenario.SECURITY_CHECK);
    }
} 