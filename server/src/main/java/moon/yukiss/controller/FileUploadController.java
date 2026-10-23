package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.common.BusinessException;
import moon.yukiss.utils.ImageTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传（头像与文章插图共用）。文件类型以文件头为准，扩展名由服务端决定。
 */
@RestController
public class FileUploadController {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final Path uploadDir;

    public FileUploadController(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片不能超过 5MB");
        }

        String extension;
        try (InputStream input = file.getInputStream()) {
            extension = ImageTypes.detectExtension(input);
        }
        if (extension == null) {
            throw new BusinessException("仅支持 JPG、PNG、GIF 或 WebP 图片");
        }

        String newFileName = UUID.randomUUID() + extension;
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(newFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException("上传路径不合法");
        }
        file.transferTo(target);

        return ApiResponse.ok(Map.of("url", "/uploads/" + newFileName));
    }
}
