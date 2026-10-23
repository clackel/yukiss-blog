package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadControllerTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @TempDir
    Path uploadDir;

    private FileUploadController controller;

    @BeforeEach
    void setUp() {
        controller = new FileUploadController(uploadDir.toString());
    }

    @Test
    void detectsTypeFromContentAndIgnoresClaimedTypeAndName() throws Exception {
        // 客户端声称是 GIF、文件名是 .html，实际内容是 PNG：以内容为准
        MockMultipartFile file = new MockMultipartFile("file", "evil.html", "image/gif", PNG);

        ApiResponse<Map<String, String>> response = controller.upload(file);

        String url = response.getData().get("url");
        assertTrue(url.matches("/uploads/[0-9a-f-]{36}\\.png"), url);
        Path stored = uploadDir.resolve(url.substring("/uploads/".length()));
        assertArrayEquals(PNG, Files.readAllBytes(stored));
    }

    @Test
    void acceptsEachSupportedImageFormat() throws Exception {
        assertTrue(upload(JPEG).endsWith(".jpg"));
        assertTrue(upload(GIF).endsWith(".gif"));
        assertTrue(upload(WEBP).endsWith(".webp"));
    }

    @Test
    void rejectsNonImageContentEvenWhenClaimedAsImage() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", html);

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.upload(file));

        assertEquals("仅支持 JPG、PNG、GIF 或 WebP 图片", exception.getMessage());
    }

    @Test
    void rejectsSvgBecauseItCanCarryScripts() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script/></svg>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg);

        assertThrows(BusinessException.class, () -> controller.upload(file));
    }

    @Test
    void rejectsTruncatedHeader() {
        MockMultipartFile file = new MockMultipartFile("file", "x.webp", "image/webp", new byte[]{'R', 'I', 'F', 'F'});

        assertThrows(BusinessException.class, () -> controller.upload(file));
    }

    private String upload(byte[] content) throws Exception {
        return controller.upload(new MockMultipartFile("file", "image", "application/octet-stream", content))
                .getData()
                .get("url");
    }
}
