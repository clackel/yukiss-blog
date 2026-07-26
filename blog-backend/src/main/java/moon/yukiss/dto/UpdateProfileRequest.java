package moon.yukiss.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Date;

@Data
public class UpdateProfileRequest {
    @Size(max = 30, message = "昵称不能超过 30 个字符")
    private String nickname;

    @Size(max = 300, message = "简介不能超过 300 个字符")
    private String bio;

    @Size(max = 20, message = "性别不能超过 20 个字符")
    private String gender;

    private Date birthday;

    @Size(max = 80, message = "所在地不能超过 80 个字符")
    private String location;

    @Size(max = 180, message = "个人网站不能超过 180 个字符")
    private String website;
}
