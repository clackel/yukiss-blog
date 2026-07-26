package moon.yukiss.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名需要 3-20 位，只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 72, message = "密码长度需要在 8 到 72 位之间")
    private String password;

    @Size(max = 30, message = "昵称不能超过 30 个字符")
    private String nickname;

    @Email(message = "请输入有效邮箱地址")
    @Size(max = 120, message = "邮箱不能超过 120 个字符")
    private String email;
}
