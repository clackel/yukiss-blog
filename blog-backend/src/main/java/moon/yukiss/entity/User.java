package moon.yukiss.entity;


import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private Date createTime;
}
