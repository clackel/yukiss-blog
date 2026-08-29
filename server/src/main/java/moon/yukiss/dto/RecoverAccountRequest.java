package moon.yukiss.dto;

import lombok.Data;

@Data
public class RecoverAccountRequest {
    private String email;
    private String code;
}
