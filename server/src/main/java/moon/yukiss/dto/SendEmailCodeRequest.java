package moon.yukiss.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailCodeRequest {
    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "请输入有效邮箱地址")
    private String email;
}
