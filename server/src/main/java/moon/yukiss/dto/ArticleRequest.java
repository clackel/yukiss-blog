package moon.yukiss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ArticleRequest {
    @NotBlank(message = "请填写文章标题")
    @Size(max = 80, message = "文章标题不能超过 80 个字符")
    private String title;

    /** 发布时必填，草稿可以为空（在 service 中校验） */
    @Size(max = 50000, message = "文章正文不能超过 50000 个字符")
    private String content;

    /** published（默认）或 draft */
    private String status;

    @Size(max = 5, message = "每篇文章最多 5 个标签")
    private List<String> tags;
}
