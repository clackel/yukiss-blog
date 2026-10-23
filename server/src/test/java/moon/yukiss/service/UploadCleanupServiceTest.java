package moon.yukiss.service;

import moon.yukiss.mapper.UploadReferenceMapper;
import moon.yukiss.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadCleanupServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T03:30:00Z");
    private static final String AVATAR = "11111111-1111-1111-1111-111111111111.png";
    private static final String ARTICLE_IMAGE = "22222222-2222-2222-2222-222222222222.jpg";
    private static final String ORPHAN = "33333333-3333-3333-3333-333333333333.webp";
    private static final String RECENT_ORPHAN = "44444444-4444-4444-4444-444444444444.gif";
    private static final String NOT_OURS = "readme.txt";

    @TempDir
    Path uploadDir;

    private UploadReferenceMapper referenceMapper;
    private UploadCleanupService service;

    @BeforeEach
    void setUp() throws Exception {
        referenceMapper = mock(UploadReferenceMapper.class);
        service = new UploadCleanupService(uploadDir, referenceMapper, new MutableClock(NOW));

        Instant old = NOW.minus(Duration.ofDays(3));
        createFile(AVATAR, old);
        createFile(ARTICLE_IMAGE, old);
        createFile(ORPHAN, old);
        createFile(NOT_OURS, old);
        createFile(RECENT_ORPHAN, NOW.minus(Duration.ofHours(2)));

        // 头像可能保存为完整地址，文章中为 Markdown 图片语法
        when(referenceMapper.findAvatarUrls()).thenReturn(List.of("http://localhost:4000/uploads/" + AVATAR));
        when(referenceMapper.findArticleContents())
                .thenReturn(List.of("正文\n\n![配图](/uploads/" + ARTICLE_IMAGE + ")\n"));
    }

    @Test
    void deletesOnlyOldUnreferencedManagedFiles() throws Exception {
        int deleted = service.cleanup();

        assertEquals(1, deleted);
        assertFalse(Files.exists(uploadDir.resolve(ORPHAN)));
        assertTrue(Files.exists(uploadDir.resolve(AVATAR)));
        assertTrue(Files.exists(uploadDir.resolve(ARTICLE_IMAGE)));
        // 24 小时内上传的文件可能属于尚未保存的草稿
        assertTrue(Files.exists(uploadDir.resolve(RECENT_ORPHAN)));
        // 不是上传接口生成的文件名，不处理
        assertTrue(Files.exists(uploadDir.resolve(NOT_OURS)));
    }

    @Test
    void missingUploadDirectoryIsNotAnError() throws Exception {
        UploadCleanupService missing = new UploadCleanupService(
                uploadDir.resolve("does-not-exist"), referenceMapper, new MutableClock(NOW));

        assertEquals(0, missing.cleanup());
    }

    private void createFile(String name, Instant modified) throws Exception {
        Path file = Files.writeString(uploadDir.resolve(name), "x");
        Files.setLastModifiedTime(file, FileTime.from(modified));
    }
}
