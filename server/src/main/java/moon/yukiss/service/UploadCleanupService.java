package moon.yukiss.service;

import moon.yukiss.mapper.UploadReferenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 定期删除不再被头像或文章引用的上传图片（换头像留下的旧图、插入后又删掉的文章配图等）。
 * 只处理由上传接口生成的文件名（UUID + 图片扩展名），并跳过最近上传的文件，
 * 避免误删正在编辑、尚未保存的文章里的图片。
 */
@Service
public class UploadCleanupService {
    static final Duration GRACE_PERIOD = Duration.ofHours(24);
    private static final String FILE_NAME = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|png|gif|webp)";
    private static final Pattern MANAGED_FILE = Pattern.compile("^" + FILE_NAME + "$");
    private static final Pattern UPLOAD_REFERENCE = Pattern.compile("/uploads/(" + FILE_NAME + ")");
    private static final Logger log = LoggerFactory.getLogger(UploadCleanupService.class);

    private final Path uploadDir;
    private final UploadReferenceMapper referenceMapper;
    private final Clock clock;

    @Autowired
    public UploadCleanupService(@Value("${app.upload.dir}") String uploadDir, UploadReferenceMapper referenceMapper) {
        this(Paths.get(uploadDir).toAbsolutePath().normalize(), referenceMapper, Clock.systemUTC());
    }

    UploadCleanupService(Path uploadDir, UploadReferenceMapper referenceMapper, Clock clock) {
        this.uploadDir = uploadDir;
        this.referenceMapper = referenceMapper;
        this.clock = clock;
    }

    /** 每天凌晨 3:30 执行 */
    @Scheduled(cron = "${app.upload.cleanup-cron:0 30 3 * * *}")
    public void scheduledCleanup() {
        try {
            int deleted = cleanup();
            if (deleted > 0) {
                log.info("Removed {} unreferenced upload(s) from {}", deleted, uploadDir);
            }
        } catch (IOException ex) {
            log.warn("Upload cleanup failed", ex);
        }
    }

    /** 返回删除的文件数 */
    public int cleanup() throws IOException {
        if (!Files.isDirectory(uploadDir)) {
            return 0;
        }
        Set<String> referenced = collectReferencedFileNames();
        Instant cutoff = clock.instant().minus(GRACE_PERIOD);
        int deleted = 0;
        try (Stream<Path> files = Files.list(uploadDir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!Files.isRegularFile(file)
                        || !MANAGED_FILE.matcher(name).matches()
                        || referenced.contains(name)
                        || !Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                    continue;
                }
                if (Files.deleteIfExists(file)) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    private Set<String> collectReferencedFileNames() {
        Set<String> names = new HashSet<>();
        addReferences(names, referenceMapper.findAvatarUrls());
        addReferences(names, referenceMapper.findArticleContents());
        return names;
    }

    private void addReferences(Set<String> names, List<String> sources) {
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            Matcher matcher = UPLOAD_REFERENCE.matcher(source);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
    }
}
