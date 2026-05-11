package com.shortlink.dto.req;

import com.shortlink.validation.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "用户信息修改请求DTO")
public class UserUpdateReqDTO {

    @Schema(description = "手机号")
    @Phone(message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String mail;

    @Schema(description = "旧密码（修改密码时必填）")
    private String oldPassword;

    @Schema(description = "新密码（修改密码时必填）")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*#?&]{6,20}$", 
             message = "密码格式不正确，需6-20位，包含字母和数字")
    private String newPassword;
}
