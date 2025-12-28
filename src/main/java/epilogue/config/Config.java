package epilogue.config;

import java.io.*;
import java.util.ArrayList;
import epilogue.crypto.MultiLayerEncryptor;
import com.google.gson.*;
import epilogue.Epilogue;
import epilogue.mixin.IAccessorMinecraft;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import epilogue.value.Value;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public class Config {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String name;
    public File file;

    // 默认密钥标识符
    private static boolean usingDefaultKeys = true;

    public Config(String name) {
        this(name, false);
    }

    public Config(String name, boolean newConfig) {
        this.name = name;
        this.file = new File("./Epilogue/", String.format("%s.json", this.name));

        try {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (newConfig && !file.exists()) {
                // 创建新的默认配置文件
                JsonObject defaultConfig = createDefaultConfig();
                saveJson(defaultConfig);
                getLogger().info("Created new config: " + file.getName());
            }
        } catch (Exception e) {
            getLogger().error("Error initializing config: " + e.getMessage());
        }
    }

    // 设置加密密钥
    public static void setEncryptionKeys(String des, String aes, String rc4,
                                         String blowfish,
                                         String xor, String custom) {
        MultiLayerEncryptor.setKeys(des, aes, rc4, blowfish, xor, custom, null, null, null, null);
        usingDefaultKeys = false;
        getLogger().info("Using custom encryption keys");
    }

    public static void setEncryptionKeys(String des, String aes, String rc4,
                                         String blowfish,
                                         String xor, String custom,
                                         String rsaPubB64, String rsaPriB64,
                                         String sm2PubB64, String sm2PriB64) {
        MultiLayerEncryptor.setKeys(des, aes, rc4, blowfish, xor, custom, rsaPubB64, rsaPriB64, sm2PubB64, sm2PriB64);
        usingDefaultKeys = false;
        getLogger().info("Using custom encryption keys");
    }

    // 重置为默认密钥
    public static void resetToDefaultKeys() {
        // 重新初始化MultiLayerEncryptor会使用默认密钥
        try {
            Class.forName("epilogue.crypto.MultiLayerEncryptor");
            usingDefaultKeys = true;
            getLogger().info("Reset to default encryption keys");
        } catch (Exception e) {
            getLogger().error("Failed to reset keys: " + e.getMessage());
        }
    }

    private static Logger getLogger() {
        try {
            return ((IAccessorMinecraft) mc).getLogger();
        } catch (Exception e) {
            return org.apache.logging.log4j.LogManager.getLogger("EpilogueConfig");
        }
    }

    public void load() {
        if (!file.exists()) {
            ChatUtil.sendFormatted(String.format("%sConfig file not found: %s",
                    Epilogue.clientName, file.getName()));
            return;
        }

        try {
            // 1. 读取文件
            String encryptedContent = readFileContent();
            if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
                ChatUtil.sendFormatted(String.format("%sConfig file is empty: %s",
                        Epilogue.clientName, file.getName()));
                return;
            }

            // 2. 尝试解密
            String decryptedContent = null;
            boolean usedDefaultKeys = false;

            try {
                // 首先使用当前密钥尝试
                decryptedContent = MultiLayerEncryptor.decrypt(encryptedContent.trim());
            } catch (Exception e) {
                getLogger().warn("Decryption failed with current keys: " + e.getMessage());

                // 如果是填充错误，尝试使用默认密钥
                if (e.getMessage() != null && e.getMessage().contains("padding")) {
                    getLogger().info("Trying with default keys...");

                    // 保存当前密钥状态
                    boolean wasUsingDefault = usingDefaultKeys;

                    // 重置到默认密钥
                    resetToDefaultKeys();
                    usedDefaultKeys = true;

                    try {
                        decryptedContent = MultiLayerEncryptor.decrypt(encryptedContent.trim());
                        getLogger().info("Successfully decrypted with default keys");
                    } catch (Exception e2) {
                        // 如果默认密钥也失败，尝试其他方法
                        throw new Exception("Failed to decrypt with both current and default keys. " +
                                "The config file may be corrupted or use different keys.", e2);
                    }

                    // 恢复之前的密钥状态
                    if (!wasUsingDefault) {
                        usingDefaultKeys = false;
                    }
                } else {
                    throw e;
                }
            }

            // 3. 解析JSON
            JsonObject jsonObject = parseJson(decryptedContent);

            // 4. 加载配置
            loadConfiguration(jsonObject);

            String keyInfo = usedDefaultKeys ? " (using default keys)" : "";
            ChatUtil.sendFormatted(String.format("%sConfig loaded successfully%s: %s",
                    Epilogue.clientName, keyInfo, file.getName()));

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("padding") || errorMsg.contains("BadPaddingException")) {
                errorMsg = "Wrong encryption key or corrupted file";
            }

            getLogger().error("Failed to load config: " + errorMsg, e);
            ChatUtil.sendFormatted(String.format("%sFailed to load config: %s",
                    Epilogue.clientName, errorMsg));
        }
    }

    private String readFileContent() throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private JsonObject parseJson(String jsonString) throws JsonSyntaxException {
        // 清理可能的BOM和空白字符
        jsonString = jsonString.trim();
        if (jsonString.startsWith("\uFEFF")) {
            jsonString = jsonString.substring(1);
        }

        JsonElement element = new JsonParser().parse(jsonString);
        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("Expected JSON object");
        }

        return element.getAsJsonObject();
    }

    private void loadConfiguration(JsonObject json) {
        // 加载账户配置（现在从独立文件加载）
        AccountConfig.load(null); // 传递null，让AccountConfig从独立文件加载

        // 加载模块配置
        for (Module module : Epilogue.moduleManager.modules.values()) {
            String moduleName = module.getName();
            if (json.has(moduleName)) {
                JsonElement moduleElement = json.get(moduleName);
                if (moduleElement.isJsonObject()) {
                    loadModule(module, moduleElement.getAsJsonObject());
                }
            }
        }
    }

    private void loadModule(Module module, JsonObject obj) {
        try {
            // 基本属性
            if (obj.has("toggled") && obj.get("toggled").isJsonPrimitive()) {
                module.setEnabled(obj.get("toggled").getAsBoolean());
            }

            if (obj.has("key") && obj.get("key").isJsonPrimitive()) {
                module.setKey(obj.get("key").getAsInt());
            }

            if (obj.has("hidden") && obj.get("hidden").isJsonPrimitive()) {
                module.setHidden(obj.get("hidden").getAsBoolean());
            }

            // 值配置
            ArrayList<Value<?>> values = Epilogue.valueHandler.properties.get(module.getClass());
            if (values != null) {
                for (Value<?> value : values) {
                    if (obj.has(value.getName())) {
                        try {
                            value.read(obj);
                        } catch (Exception e) {
                            getLogger().warn("Failed to load value " + value.getName() +
                                    " for module " + module.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().error("Error loading module " + module.getName() + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            // 确保目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 构建配置JSON
            JsonObject configJson = new JsonObject();

            // 账户配置（现在只保存引用信息）
            configJson.add("accountManager", AccountConfig.save());

            // 模块配置
            for (Module module : Epilogue.moduleManager.modules.values()) {
                configJson.add(module.getName(), saveModule(module));
            }

            // 保存主配置文件
            saveJson(configJson);

            // 同时保存账户到独立文件
            AccountConfig.saveToFile();

            ChatUtil.sendFormatted(String.format("%sConfig saved successfully: %s",
                    Epilogue.clientName, file.getName()));

        } catch (Exception e) {
            getLogger().error("Failed to save config: " + e.getMessage(), e);
            ChatUtil.sendFormatted(String.format("%sFailed to save config: %s",
                    Epilogue.clientName, e.getMessage()));
        }
    }

    private JsonObject saveModule(Module module) {
        JsonObject obj = new JsonObject();

        obj.addProperty("toggled", module.isEnabled());
        obj.addProperty("key", module.getKey());
        obj.addProperty("hidden", module.isHidden());

        // 保存值配置
        ArrayList<Value<?>> values = Epilogue.valueHandler.properties.get(module.getClass());
        if (values != null) {
            for (Value<?> value : values) {
                try {
                    value.write(obj);
                } catch (Exception e) {
                    getLogger().warn("Failed to save value " + value.getName() +
                            " for module " + module.getName() + ": " + e.getMessage());
                }
            }
        }

        return obj;
    }

    private JsonObject createDefaultConfig() {
        JsonObject config = new JsonObject();
        // 账户配置只保存引用信息
        config.add("accountManager", AccountConfig.save());

        // 为每个模块创建默认配置
        for (Module module : Epilogue.moduleManager.modules.values()) {
            JsonObject moduleConfig = new JsonObject();
            moduleConfig.addProperty("toggled", false);
            moduleConfig.addProperty("key", 0);
            moduleConfig.addProperty("hidden", false);
            config.add(module.getName(), moduleConfig);
        }

        return config;
    }

    private void saveJson(JsonObject json) throws Exception {
        // 转换为JSON字符串
        String jsonString = gson.toJson(json);

        // 加密
        String encrypted;
        try {
            encrypted = MultiLayerEncryptor.encrypt(jsonString);
        } catch (Exception e) {
            getLogger().error("Encryption failed: " + e.getMessage());
            throw new Exception("Failed to encrypt config: " + e.getMessage(), e);
        }

        // 写入文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(encrypted);
        }
    }
}