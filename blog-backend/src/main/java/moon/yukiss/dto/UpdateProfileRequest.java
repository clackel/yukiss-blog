package moon.yukiss.dto;

import lombok.Data;
import java.util.Date;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String bio;
    private String gender;
    private Date birthday;
    private String location;
    private String website;
}
