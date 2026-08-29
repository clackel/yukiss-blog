package moon.yukiss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "请选择要评论的文章")
    @Positive(message = "文章 ID 不合法")
    private Integer articleId;

    @Positive(message = "父评论 ID 不合法")
    private Integer parentId;

    @NotBlank(message = "请填写评论内容")
    @Size(max = 500, message = "评论不能超过 500 个字符")
    private String content;
}
