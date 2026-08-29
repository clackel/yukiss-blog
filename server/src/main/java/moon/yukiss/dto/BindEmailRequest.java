package moon.yukiss.dto;

import lombok.Data;

@Data
public class BindEmailRequest {
    private String email;
    private String code;
}
