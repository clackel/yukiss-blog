package moon.yukiss.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 根据文件头（魔数）识别图片类型，不信任客户端上报的 Content-Type 和文件名。
 */
public final class ImageTypes {
    private static final int HEADER_LENGTH = 12;

    private ImageTypes() {
    }

    /** 返回扩展名（含点），无法识别为支持的图片时返回 null。 */
    public static String detectExtension(InputStream input) throws IOException {
        byte[] header = input.readNBytes(HEADER_LENGTH);
        return detectExtension(header);
    }

    static String detectExtension(byte[] header) {
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return ".jpg";
        }
        if (startsWith(header, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return ".png";
        }
        if (startsWith(header, 'G', 'I', 'F', '8', '7', 'a') || startsWith(header, 'G', 'I', 'F', '8', '9', 'a')) {
            return ".gif";
        }
        if (header.length >= 12
                && startsWith(header, 'R', 'I', 'F', 'F')
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), new byte[]{'W', 'E', 'B', 'P'})) {
            return ".webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
