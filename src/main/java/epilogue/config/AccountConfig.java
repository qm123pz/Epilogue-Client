package epilogue.config;

import com.google.gson.*;
import epilogue.crypto.MultiLayerEncryptor;
import epilogue.ui.mainmenu.altmanager.auth.Account;
import epilogue.Epilogue;
import epilogue.util.ChatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountConfig {
    private static final List<Account> accounts = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("EpilogueAccountConfig");
    private static boolean needsEncryptionSave = false;
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 独立的账户配置文件
    private static final File ACCOUNTS_FILE = new File("./accounts.json");

    // 初始化方法
    public static void init() {
        try {
            File parentDir = ACCOUNTS_FILE.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize account config directory: " + e.getMessage());
        }
    }

    // 保存账户到独立文件
    public static void saveToFile() {
        try {
            // 创建账户数据JSON
            JsonObject configObject = new JsonObject();
            JsonArray jsonArray = createAccountsJsonArray();
            configObject.add("accounts", jsonArray);

            // 加密整个账户数据
            String jsonString = gson.toJson(configObject);
            String encryptedData = MultiLayerEncryptor.encrypt(jsonString);

            JsonObject encryptedObject = new JsonObject();
            encryptedObject.addProperty("encryptedAccounts", encryptedData);
            encryptedObject.addProperty("version", "1.0");
            encryptedObject.addProperty("encryptionType", "MultiLayer");
            encryptedObject.addProperty("lastModified", System.currentTimeMillis());

            // 写入文件
            String finalJson = gson.toJson(encryptedObject);
            try (PrintWriter writer = new PrintWriter(new FileWriter(ACCOUNTS_FILE))) {
                writer.print(finalJson);
            }

            LOGGER.info("Saved " + accounts.size() + " accounts to separate file");
            needsEncryptionSave = false;

        } catch (Exception e) {
            LOGGER.error("Failed to save account config to file: " + e.getMessage(), e);
            ChatUtil.sendFormatted(String.format("%sFailed to save accounts: %s",
                    Epilogue.clientName, e.getMessage()));
        }
    }

    // 从独立文件加载账户
    public static void loadFromFile() {
        accounts.clear();
        needsEncryptionSave = false;

        if (!ACCOUNTS_FILE.exists()) {
            LOGGER.info("Accounts file does not exist, will be created on save");
            return;
        }

        try {
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(ACCOUNTS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            if (content.length() == 0) {
                LOGGER.info("Accounts file is empty");
                return;
            }

            // 解析JSON
            JsonObject object = new JsonParser().parse(content.toString()).getAsJsonObject();

            // 加载加密的账户数据
            if (object.has("encryptedAccounts") && object.get("encryptedAccounts").isJsonPrimitive()) {
                String encryptedData = object.get("encryptedAccounts").getAsString();
                if (encryptedData == null || encryptedData.trim().isEmpty()) {
                    LOGGER.warn("Encrypted accounts data is empty");
                    return;
                }

                try {
                    String decryptedJson = MultiLayerEncryptor.decrypt(encryptedData.trim());
                    JsonObject decryptedObject = new JsonParser().parse(decryptedJson).getAsJsonObject();
                    loadAccountsFromJson(decryptedObject);
                    LOGGER.info("Successfully decrypted " + accounts.size() + " accounts from file");
                } catch (Exception e) {
                    LOGGER.error("Failed to decrypt account data from file: " + e.getMessage());
                    ChatUtil.sendFormatted(String.format("%sFailed to load accounts: Invalid encryption key",
                            Epilogue.clientName));
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load account config from file: " + e.getMessage(), e);
        }
    }

    // 提供给 Config 类的接口 - 现在返回空对象
    public static JsonObject save() {
        // 账户数据现在保存在独立文件，这里返回空对象
        JsonObject emptyObject = new JsonObject();
        emptyObject.addProperty("separateFile", true);
        emptyObject.addProperty("accountsFile", "accounts.epc");
        return emptyObject;
    }

    // 提供给 Config 类的接口 - 现在从独立文件加载
    public static void load(JsonObject object) {
        // 忽略传入的对象，直接从独立文件加载
        loadFromFile();
    }

    // 保持原有的账户处理方法
    private static void loadAccountsFromJson(JsonObject object) {
        if (object == null) return;

        JsonArray jsonArray = object.getAsJsonArray("accounts");
        if (jsonArray != null) {
            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    try {
                        Account account = parseAccountFromJson(jsonObject);
                        if (account != null) {
                            accounts.add(account);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse account entry: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static Account parseAccountFromJson(JsonObject jsonObject) {
        String refreshToken = getStringProperty(jsonObject, "refreshToken", "");
        String accessToken = getStringProperty(jsonObject, "accessToken", "");
        String username = getStringProperty(jsonObject, "username", "");
        long timestamp = getLongProperty(jsonObject, "timestamp", System.currentTimeMillis());
        String uuid = getStringProperty(jsonObject, "uuid", "");

        if (username.isEmpty() && refreshToken.isEmpty() && accessToken.isEmpty()) {
            LOGGER.warn("Skipping invalid account entry (missing required fields)");
            return null;
        }

        return new Account(refreshToken, accessToken, username, timestamp, uuid);
    }

    private static JsonArray createAccountsJsonArray() {
        JsonArray jsonArray = new JsonArray();
        for (Account account : accounts) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("refreshToken", account.getRefreshToken());
            jsonObject.addProperty("accessToken", account.getAccessToken());
            jsonObject.addProperty("username", account.getUsername());
            jsonObject.addProperty("timestamp", account.getTimestamp());
            jsonObject.addProperty("uuid", account.getUUID());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private static String getStringProperty(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key)) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }
        return defaultValue;
    }

    private static long getLongProperty(JsonObject obj, String key, long defaultValue) {
        if (obj.has(key)) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive()) {
                try {
                    return element.getAsLong();
                } catch (Exception e) {
                    try {
                        return Long.parseLong(element.getAsString());
                    } catch (Exception e2) {
                        // 忽略
                    }
                }
            }
        }
        return defaultValue;
    }

    // 账户操作方法 - 保持原有逻辑，但添加自动保存
    public static Account get(int index) {
        if (index >= 0 && index < accounts.size()) {
            return accounts.get(index);
        }
        return null;
    }

    public static void add(Account account) {
        if (account != null) {
            accounts.add(account);
            saveToFile();
        }
    }

    public static void remove(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            saveToFile();
        }
    }

    public static void swap(int i, int j) {
        if (i >= 0 && i < accounts.size() && j >= 0 && j < accounts.size()) {
            Collections.swap(accounts, i, j);
            saveToFile();
        }
    }

    public static void clear() {
        accounts.clear();
        saveToFile();
    }

    public static int size() {
        return accounts.size();
    }

    public static List<Account> all() {
        return new ArrayList<>(accounts);
    }

    public static boolean needsEncryptionConversion() {
        return needsEncryptionSave;
    }
}