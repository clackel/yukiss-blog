package moon.yukiss.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@CrossOrigin
@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public String upload(MultipartFile file) throws IOException {
        // 1. 获取文件的原始名字 (比如 avatar.png)
        String originalFilename = file.getOriginalFilename();

        // 2. 为了防止不同用户上传同名图片产生覆盖，我们用 UUID 给图片重新起个独一无二的名字
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extension;

        // 3. 确定保存的文件夹路径
        String folderPath = System.getProperty("user.dir") + "/uploads/";

        // 4. 把文件保存到电脑上
        file.transferTo(new File(folderPath + newFileName));

        // 5. 返回能访问到这张图片的 URL (注意端口号要和你后端的一致，这里假设是 4000)
        return "/uploads/" + newFileName;
    }
}