package epilogue.config;

import java.io.*;
import java.util.ArrayList;
import com.google.gson.*;
import epilogue.Epilogue;
import epiloguemixinbridge.IAccessorMinecraft;
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

    private static final int SCHEMA_VERSION = 1;

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
                JsonObject defaultConfig = createDefaultConfig();
                saveJson(defaultConfig);
                getLogger().info("Created new config: " + file.getName());
            }
        } catch (Exception e) {
            getLogger().error("Error initializing config: " + e.getMessage());
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
            String content = readFileContent();
            if (content == null || content.trim().isEmpty()) {
                ChatUtil.sendFormatted(String.format("%sConfig file is empty: %s",
                        Epilogue.clientName, file.getName()));
                return;
            }

            JsonObject jsonObject = parseJson(content);
            loadConfiguration(jsonObject);

            ChatUtil.sendFormatted(String.format("%sConfig loaded successfully: %s",
                    Epilogue.clientName, file.getName()));

        } catch (Exception e) {
            getLogger().error("Failed to load config: " + e.getMessage(), e);
            ChatUtil.sendFormatted(String.format("%sFailed to load config: %s",
                    Epilogue.clientName, e.getMessage()));
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
        if (!json.has("meta") || !json.get("meta").isJsonObject()) {
            throw new JsonSyntaxException("Missing 'meta' object");
        }

        JsonObject meta = json.getAsJsonObject("meta");
        if (!meta.has("schema") || meta.get("schema").getAsInt() != SCHEMA_VERSION) {
            throw new JsonSyntaxException("Unsupported schema version");
        }

        AccountConfig.load(null);
        if (!json.has("modules") || !json.get("modules").isJsonObject()) {
            throw new JsonSyntaxException("Missing 'modules' object");
        }

        JsonObject modules = json.getAsJsonObject("modules");
        for (Module module : Epilogue.moduleManager.modules.values()) {
            String moduleName = module.getName();
            if (modules.has(moduleName)) {
                JsonElement moduleElement = modules.get(moduleName);
                if (moduleElement.isJsonObject()) {
                    loadModule(module, moduleElement.getAsJsonObject());
                }
            }
        }
    }

    private void loadModule(Module module, JsonObject obj) {
        try {
            if (obj.has("toggled") && obj.get("toggled").isJsonPrimitive()) {
                module.setEnabled(obj.get("toggled").getAsBoolean());
            }

            if (obj.has("key") && obj.get("key").isJsonPrimitive()) {
                module.setKey(obj.get("key").getAsInt());
            }

            if (obj.has("hidden") && obj.get("hidden").isJsonPrimitive()) {
                module.setHidden(obj.get("hidden").getAsBoolean());
            }
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
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            JsonObject configJson = new JsonObject();

            JsonObject meta = new JsonObject();
            meta.addProperty("schema", SCHEMA_VERSION);
            meta.addProperty("name", this.name);
            meta.addProperty("savedAt", System.currentTimeMillis());
            configJson.add("meta", meta);

            configJson.add("accountManager", AccountConfig.save());

            JsonObject modules = new JsonObject();
            for (Module module : Epilogue.moduleManager.modules.values()) {
                modules.add(module.getName(), saveModule(module));
            }
            configJson.add("modules", modules);

            saveJson(configJson);
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

        JsonObject meta = new JsonObject();
        meta.addProperty("schema", SCHEMA_VERSION);
        meta.addProperty("name", this.name);
        meta.addProperty("savedAt", System.currentTimeMillis());
        config.add("meta", meta);

        config.add("accountManager", AccountConfig.save());

        JsonObject modules = new JsonObject();
        for (Module module : Epilogue.moduleManager.modules.values()) {
            JsonObject moduleConfig = new JsonObject();
            moduleConfig.addProperty("toggled", false);
            moduleConfig.addProperty("key", 0);
            moduleConfig.addProperty("hidden", false);
            modules.add(module.getName(), moduleConfig);
        }
        config.add("modules", modules);

        return config;
    }

    private void saveJson(JsonObject json) throws Exception {
        String jsonString = gson.toJson(json);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(jsonString);
        }
    }
}