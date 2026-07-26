package moon.yukiss.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RecoverAccountRequest {
    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "请输入有效邮箱地址")
    private String email;

    @NotBlank(message = "请输入验证码")
    @Pattern(regexp = "^\\d{6}$", message = "验证码应为 6 位数字")
    private String code;
}
