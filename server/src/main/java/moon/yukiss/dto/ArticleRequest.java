package moon.yukiss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleRequest {
    @NotBlank(message = "请填写文章标题")
    @Size(max = 80, message = "文章标题不能超过 80 个字符")
    private String title;

    @NotBlank(message = "请填写文章正文")
    @Size(max = 50000, message = "文章正文不能超过 50000 个字符")
    private String content;
}
