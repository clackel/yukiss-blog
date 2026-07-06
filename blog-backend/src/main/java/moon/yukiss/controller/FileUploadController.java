package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class FileUploadController {
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Path uploadDir;

    public FileUploadController(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("头像仅支持 JPG、PNG、GIF 或 WebP 图片");
        }

        String extension = extensionFor(contentType);
        String newFileName = UUID.randomUUID() + extension;
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(newFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException("上传路径不合法");
        }
        file.transferTo(target);

        return ApiResponse.ok(Map.of("url", "/uploads/" + newFileName));
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException("不支持的图片格式");
        };
    }
}
