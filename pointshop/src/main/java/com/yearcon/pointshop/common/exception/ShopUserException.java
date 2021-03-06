package com.yearcon.pointshop.common.exception;

import com.yearcon.pointshop.common.enums.ResultEnum;
import lombok.Data;
import org.springframework.security.core.AuthenticationException;

/**
 * @author itguang
 * @create 2018-01-06 14:31
 **/
@Data
public class ShopUserException extends AuthenticationException {


    private Integer code;

    public ShopUserException(ResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.code = resultEnum.getCode();
    }
}
