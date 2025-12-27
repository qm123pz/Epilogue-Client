package epilogue.ncm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestUtil {

    private static final SecureRandom random = new SecureRandom();
    private static final Gson gson = new Gson();

    private static final Map<String, OsConfig> OS_MAP = new HashMap<>();
    private static final Map<String, Map<String, String>> USER_AGENT_MAP = new HashMap<>();

    public static String globalDeviceId = "";

    static {
        Map<String, String> weapiUA = new HashMap<>();
        weapiUA.put("pc", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0");
        USER_AGENT_MAP.put("weapi", weapiUA);

        Map<String, String> linuxapiUA = new HashMap<>();
        linuxapiUA.put("linux", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36");
        USER_AGENT_MAP.put("linuxapi", linuxapiUA);

        Map<String, String> apiUA = new HashMap<>();
        apiUA.put("pc", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152");
        apiUA.put("android", "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001)");
        apiUA.put("iphone", "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)");
        USER_AGENT_MAP.put("api", apiUA);

        OS_MAP.put("pc", new OsConfig("pc", "3.1.12.204072", "Microsoft-Windows-11-Professional-build-26100-64bit", "netease"));
        OS_MAP.put("linux", new OsConfig("linux", "1.2.1.0428", "Deepin 20.9", "netease"));
        OS_MAP.put("android", new OsConfig("android", "8.20.20.231215173437", "14", "xiaomi"));
        OS_MAP.put("iphone", new OsConfig("iPhone OS", "9.0.90", "16.2", "distribution"));
    }

    public static class AppConfig {
        private final String domain;
        private final String apiDomain;
        private final boolean encrypt;
        private final boolean encryptResponse;

        public AppConfig(String domain, String apiDomain, boolean encrypt, boolean encryptResponse) {
            this.domain = domain;
            this.apiDomain = apiDomain;
            this.encrypt = encrypt;
            this.encryptResponse = encryptResponse;
        }

        public String getDomain() {
            return domain;
        }

        public String getApiDomain() {
            return apiDomain;
        }

        public boolean isEncrypt() {
            return encrypt;
        }

        public boolean isEncryptResponse() {
            return encryptResponse;
        }
    }

    private static final AppConfig APP_CONF = new AppConfig(
            "https://music.163.com",
            "https://interfacepc.music.163.com",
            true,
            false
    );

    public static class OsConfig {
        private final String os;
        private final String appver;
        private final String osver;
        private final String channel;

        public OsConfig(String os, String appver, String osver, String channel) {
            this.os = os;
            this.appver = appver;
            this.osver = osver;
            this.channel = channel;
        }

        public String getOs() {
            return os;
        }

        public String getAppver() {
            return appver;
        }

        public String getOsver() {
            return osver;
        }

        public String getChannel() {
            return channel;
        }
    }

    public static class RequestOptions {
        private Map<String, String> headers;
        private String realIP;
        private String ip;
        private Object cookie;
        private String crypto;
        private String ua;
        private String proxy;
        private Boolean eR;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final RequestOptions o = new RequestOptions();

            public Builder headers(Map<String, String> headers) {
                o.headers = headers;
                return this;
            }

            public Builder realIP(String realIP) {
                o.realIP = realIP;
                return this;
            }

            public Builder ip(String ip) {
                o.ip = ip;
                return this;
            }

            public Builder cookie(Object cookie) {
                o.cookie = cookie;
                return this;
            }

            public Builder crypto(String crypto) {
                o.crypto = crypto;
                return this;
            }

            public Builder ua(String ua) {
                o.ua = ua;
                return this;
            }

            public Builder proxy(String proxy) {
                o.proxy = proxy;
                return this;
            }

            public Builder eR(Boolean eR) {
                o.eR = eR;
                return this;
            }

            public RequestOptions build() {
                return o;
            }
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getRealIP() {
            return realIP;
        }

        public String getIp() {
            return ip;
        }

        public Object getCookie() {
            return cookie;
        }

        public String getCrypto() {
            return crypto;
        }

        public String getUa() {
            return ua;
        }

        public String getProxy() {
            return proxy;
        }

        public Boolean getER() {
            return eR;
        }
    }

    public static class RequestAnswer {
        private int status;
        private Object body;
        private String[] cookies;

        public RequestAnswer(int status, Object body, String[] cookies) {
            this.status = status;
            this.body = body;
            this.cookies = cookies;
        }

        public static RequestAnswer of(JsonObject object, int status, String[] cookies) {
            return new RequestAnswer(status, object, cookies);
        }

        public int getStatus() {
            return status;
        }

        public Object getBody() {
            return body;
        }

        public String[] getCookies() {
            return cookies;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public void setCookies(String[] cookies) {
            this.cookies = cookies;
        }

        public String toString() {
            return gson.toJson(this.body);
        }

        public JsonObject toJsonObject() {
            return gson.fromJson(toString(), JsonObject.class);
        }
    }

    private static String generateWNMCID() {
        String characters = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            randomString.append(characters.charAt(random.nextInt(characters.length())));
        }
        return randomString + "." + System.currentTimeMillis() + ".01.0";
    }

    private static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String generateRandomString(int length) {
        byte[] bytes = generateRandomBytes(length);
        return bytesToHex(bytes);
    }

    private static String chooseUserAgent(String crypto, String uaType) {
        if (uaType == null) uaType = "pc";
        Map<String, String> cryptoMap = USER_AGENT_MAP.get(crypto);
        if (cryptoMap != null) {
            return cryptoMap.getOrDefault(uaType, "");
        }
        return "";
    }

    private static Map<String, String> cookieToMap(String cookieStr) {
        Map<String, String> cookieMap = new HashMap<>();
        if (StringUtils.isNotBlank(cookieStr)) {
            String[] pairs = cookieStr.split(";\\s*");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    cookieMap.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        return cookieMap;
    }

    private static String cookieMapToString(Map<String, String> cookieMap) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildFormData(Map<String, String> formData) {
        return formData.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
                    } catch (Exception e) {
                        return entry.getKey() + "=" + entry.getValue();
                    }
                })
                .collect(Collectors.joining("&"));
    }

    public static RequestAnswer createRequest(String uri, Object data, RequestOptions options) {
        try {
            Map<String, String> headers = options.getHeaders() != null ? new HashMap<>(options.getHeaders()) : new HashMap<>();

            String ip = StringUtils.isNotBlank(options.getRealIP()) ? options.getRealIP() : options.getIp();
            if (StringUtils.isNotBlank(ip)) {
                headers.put("X-Real-IP", ip);
                headers.put("X-Forwarded-For", ip);
            }

            Map<String, String> cookie = new HashMap<>();
            if (options.getCookie() instanceof String) {
                cookie = cookieToMap((String) options.getCookie());
            } else if (options.getCookie() instanceof Map) {
                cookie = (Map<String, String>) options.getCookie();
            }

            String nuid = generateRandomString(32);
            OsConfig osConfig = OS_MAP.getOrDefault(cookie.get("os"), OS_MAP.get("pc"));

            cookie.putIfAbsent("__remember_me", "true");
            cookie.putIfAbsent("ntes_kaola_ad", "1");
            cookie.putIfAbsent("_ntes_nuid", nuid);
            cookie.putIfAbsent("_ntes_nnid", nuid + "," + System.currentTimeMillis());
            cookie.putIfAbsent("WNMCID", generateWNMCID());
            cookie.putIfAbsent("WEVNSM", "1.0.0");
            cookie.putIfAbsent("osver", osConfig.getOsver());
            cookie.putIfAbsent("deviceId", generateRandomString(26).toUpperCase() + "\\r");
            cookie.putIfAbsent("os", osConfig.getOs());
            cookie.putIfAbsent("channel", osConfig.getChannel());
            cookie.putIfAbsent("appver", osConfig.getAppver());

            if (!uri.contains("login")) {
                cookie.put("NMTID", generateRandomString(16));
            }

            headers.put("Cookie", cookieMapToString(cookie));

            String crypto = StringUtils.isNotBlank(options.getCrypto()) ? options.getCrypto() : (APP_CONF.isEncrypt() ? "eapi" : "api");
            String url;
            String postData;
            String csrfToken = cookie.getOrDefault("__csrf", "");

            switch (crypto) {
                case "weapi": {
                    headers.put("Referer", APP_CONF.getDomain());
                    headers.put("User-Agent", StringUtils.isNotBlank(options.getUa()) ? options.getUa() : chooseUserAgent("weapi", "pc"));

                    Map<String, Object> weapiData = new HashMap<>();
                    if (data instanceof Map) {
                        weapiData.putAll((Map<String, Object>) data);
                    }
                    weapiData.put("csrf_token", csrfToken);

                    CryptoUtil.WeapiResult weapiResult = CryptoUtil.weapi(weapiData);
                    url = APP_CONF.getDomain() + "/weapi/" + uri.substring(5);

                    Map<String, String> weapiFormData = new HashMap<>();
                    weapiFormData.put("params", weapiResult.getParams());
                    weapiFormData.put("encSecKey", weapiResult.getEncSecKey());
                    postData = buildFormData(weapiFormData);
                    break;
                }
                case "linuxapi": {
                    headers.put("User-Agent", StringUtils.isNotBlank(options.getUa()) ? options.getUa() : chooseUserAgent("linuxapi", "linux"));

                    Map<String, Object> linuxApiRequest = new HashMap<>();
                    linuxApiRequest.put("method", "POST");
                    linuxApiRequest.put("url", APP_CONF.getDomain() + uri);
                    linuxApiRequest.put("params", data);

                    CryptoUtil.LinuxapiResult linuxResult = CryptoUtil.linuxapi(linuxApiRequest);
                    url = APP_CONF.getDomain() + "/api/linux/forward";

                    Map<String, String> linuxFormData = new HashMap<>();
                    linuxFormData.put("eparams", linuxResult.getEparams());
                    postData = buildFormData(linuxFormData);
                    break;
                }
                case "eapi":
                case "api": {
                    Map<String, String> header = new HashMap<>();
                    header.put("osver", cookie.get("osver"));
                    header.put("deviceId", cookie.get("deviceId"));
                    header.put("os", cookie.get("os"));
                    header.put("appver", cookie.get("appver"));
                    header.put("versioncode", cookie.getOrDefault("versioncode", "140"));
                    header.put("mobilename", cookie.getOrDefault("mobilename", ""));
                    header.put("buildver", cookie.getOrDefault("buildver", String.valueOf(System.currentTimeMillis()).substring(0, 10)));
                    header.put("resolution", cookie.getOrDefault("resolution", "1920x1080"));
                    header.put("__csrf", csrfToken);
                    header.put("channel", cookie.get("channel"));
                    header.put("requestId", System.currentTimeMillis() + "_" + String.format("%04d", random.nextInt(1000)));

                    if (cookie.containsKey("MUSIC_U")) {
                        header.put("MUSIC_U", cookie.get("MUSIC_U"));
                    }
                    if (cookie.containsKey("MUSIC_A")) {
                        header.put("MUSIC_A", cookie.get("MUSIC_A"));
                    }

                    headers.put("Cookie", cookieMapToString(header));
                    headers.put("User-Agent", StringUtils.isNotBlank(options.getUa()) ? options.getUa() : chooseUserAgent("api", "pc"));

                    if ("eapi".equals(crypto)) {
                        Map<String, Object> eapiData = new HashMap<>();
                        if (data instanceof Map) {
                            eapiData.putAll((Map<String, Object>) data);
                        }
                        eapiData.put("header", header);

                        Boolean eR = options.getER() != null ? options.getER() : APP_CONF.isEncryptResponse();
                        eapiData.put("e_r", eR);

                        CryptoUtil.EapiResult eapiResult = CryptoUtil.eapi(uri, eapiData);
                        url = APP_CONF.getApiDomain() + "/eapi/" + uri.substring(5);

                        Map<String, String> eapiFormData = new HashMap<>();
                        eapiFormData.put("params", eapiResult.getParams());
                        postData = buildFormData(eapiFormData);
                    } else {
                        url = APP_CONF.getApiDomain() + uri;
                        postData = gson.toJson(data);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown crypto type: " + crypto);
            }

            URL requestUrl = new URL(url);
            HttpURLConnection connection;

            if (StringUtils.isNotBlank(options.getProxy())) {
                String[] proxyParts = options.getProxy().split(":");
                if (proxyParts.length == 2) {
                    String proxyHost = proxyParts[0];
                    int proxyPort = Integer.parseInt(proxyParts[1]);
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                    connection = (HttpURLConnection) requestUrl.openConnection(proxy);
                } else {
                    connection = (HttpURLConnection) requestUrl.openConnection();
                }
            } else {
                connection = (HttpURLConnection) requestUrl.openConnection();
            }

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            if ("api".equals(crypto)) {
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            } else {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (postData != null && !postData.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();

            String responseBody = "";
            InputStream inputStream = null;
            try {
                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        responseBody = response.toString();
                    }
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }

            RequestAnswer answer = new RequestAnswer(responseCode, null, null);

            if (!responseBody.isEmpty()) {
                if (Boolean.TRUE.equals(options.getER()) && "eapi".equals(crypto)) {
                    try {
                        answer.setBody(CryptoUtil.eapiResDecrypt(responseBody));
                    } catch (Exception e) {
                        answer.setBody(gson.fromJson(responseBody, Object.class));
                    }
                } else {
                    try {
                        answer.setBody(gson.fromJson(responseBody, Object.class));
                    } catch (Exception e) {
                        answer.setBody(responseBody);
                    }
                }
            }

            Map<String, List<String>> headerFields = connection.getHeaderFields();
            List<String> setCookieHeaders = headerFields.get("Set-Cookie");
            if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                answer.setCookies(setCookieHeaders.toArray(new String[0]));
            }

            connection.disconnect();

            return answer;

        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", 502);
            map.put("msg", e.getMessage());
            return new RequestAnswer(502, map, null);
        }
    }
}
