package moon.yukiss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentUpdateRequest {
    @NotBlank(message = "请填写评论内容")
    @Size(max = 500, message = "评论不能超过 500 个字符")
    private String content;
}
