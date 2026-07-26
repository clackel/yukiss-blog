package moon.yukiss.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    @NotBlank(message = "请输入当前密码确认注销")
    private String password;
}
