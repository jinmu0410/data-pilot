package cn.datapilot.web.vo.user;


import cn.datapilot.common.annotation.Mask;
import cn.datapilot.common.enums.MaskType;
import lombok.Data;

@Data
public class UserListRequest {

    private String username;

    @Mask(type = MaskType.EMAIL)
    private String email;

    private String avatar;

    private String status;

}
