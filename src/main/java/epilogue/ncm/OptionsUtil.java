package epilogue.ncm;

public class OptionsUtil {

    private static String COOKIE = "";

    public static void setCookie(String cookie) {
        COOKIE = cookie == null ? "" : cookie;
    }

    public static String getCookie() {
        return COOKIE;
    }

    public static RequestUtil.RequestOptions createOptions() {
        return createOptions("");
    }

    public static RequestUtil.RequestOptions createOptions(String crypto) {
        return RequestUtil.RequestOptions.builder()
                .crypto(crypto)
                .cookie(COOKIE)
                .ua("")
                .proxy("")
                .realIP("123.168.116.9")
                .eR(null)
                .build();
    }
}
