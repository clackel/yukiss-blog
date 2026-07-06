package moon.yukiss.entity;


import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;

@Data
public class User {
    private Integer id;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String email;
    private Boolean emailVerified;
    private String avatar;
    private String bio;
    private String gender;
    private Date birthday;
    private String location;
    private String website;
    private String role;
    private Date createTime;
    private Date lastLoginTime;
    private Date passwordUpdatedTime;
    private Date deletedTime;
}
