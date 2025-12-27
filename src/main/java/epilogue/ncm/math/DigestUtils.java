package epilogue.ncm.math;

import java.security.MessageDigest;

public class DigestUtils {

    private static MessageDigest getDigest(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static MessageDigest getMd5Digest() {
        return getDigest(MessageDigestAlgorithms.MD5);
    }

    public static byte[] md5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    public static byte[] md5(final String data) {
        return md5(epilogue.ncm.math.StringUtils.getBytesUtf8(data));
    }

    public static String md5Hex(final String data) {
        return Hex.encodeHexString(md5(data));
    }
}
