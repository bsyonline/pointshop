package com.yearcon.pointshop.moudles.user.web;

import com.yearcon.pointshop.common.anno.LoggerManage;
import com.yearcon.pointshop.common.config.security.TokenAuthenticationService;
import com.yearcon.pointshop.common.enums.ResultEnum;
import com.yearcon.pointshop.common.exception.ShopException;
import com.yearcon.pointshop.common.repository.mysql.crm.ShopCrmRepository;
import com.yearcon.pointshop.common.repository.mysql.user.ShopCodeRepository;
import com.yearcon.pointshop.common.utils.HttpClientUtil2;
import com.yearcon.pointshop.common.utils.RandomCode;
import com.yearcon.pointshop.common.vo.ShopResult;
import com.yearcon.pointshop.common.vo.UserSupplementVO;
import com.yearcon.pointshop.common.vo.UserVO;
import com.yearcon.pointshop.moudles.crm.entity.ShopCrmEntity;
import com.yearcon.pointshop.moudles.crm.service.ShopCrmService;
import com.yearcon.pointshop.moudles.user.entity.ShopCodeEntity;
import com.yearcon.pointshop.moudles.user.entity.ShopConfigEntity;
import com.yearcon.pointshop.moudles.user.entity.ShopCustomerEntity;
import com.yearcon.pointshop.moudles.user.service.ShopCustomerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author itguang
 * @create 2018-01-11 14:03
 **/
@RestController
@RequestMapping(value = "/user", produces = APPLICATION_JSON_VALUE)
@Api(description = "用户管理")
@Slf4j
public class ShopCustomerController {


    @Autowired
    ShopCustomerService shopCustomerService;

    @Autowired
    ShopCrmService shopCrmService;

    @Autowired
    private ShopCrmRepository shopCrmRepository;


    /**
     * 发送手机验证码
     *
     * @param phone 手机号
     * @return
     */
    @ApiOperation(value = "发送验证码", notes = "发送验证码给手机号")
    @RequestMapping(value = "/sendCode/{phone}", method = {RequestMethod.GET})
    @LoggerManage(logDescription = "发送验证码")
    public ShopResult sendCode(@ApiParam(value = "手机号", required = true) @PathVariable(name = "phone") String phone,
                               HttpSession httpSession) {

        String URL = "http://61.145.229.29:7791/MWGate/wmgw.asmx/MongateSendSubmit";

        //生成验证码
        String code = RandomCode.genCode(4);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", "H11868");
        map.put("password", "332516");
        map.put("pszMobis", phone);
        map.put("pszMsg", "您正在注册意尔康会员，验证码是：" + code);
        map.put("iMobiCount", "1");
        map.put("pszSubPort", "*");
        map.put("MsgId", String.valueOf(System.currentTimeMillis()));

        String s = null;
        try {
            s = HttpClientUtil2.httpGetRequest(URL, map);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ShopException(ResultEnum.SEND_CODE_FAILE);
        }


        log.info("response====={}", s);

        //把验证码保存起来,这里存放数据库中

        shopCustomerService.saveShopCodeEntity(phone, code);


        return ShopResult.success();
    }


    /**
     * 手机号注册
     *
     * @param phone 手机号
     * @param code  验证码
     * @return
     */
    @ApiOperation(value = "用户注册", notes = "用户手机号注册,参数都为必填")
    @RequestMapping(value = "/register/{openid}", method = RequestMethod.POST)
    @LoggerManage(logDescription = "用户注册")
    public ShopResult register(@PathVariable(name = "openid") String openid,
                               @ApiParam(value = "手机号", required = true) @RequestParam(name = "phone") String phone,
                               @ApiParam(value = "验证码", required = true) @RequestParam(name = "code") String code,
                               HttpServletResponse response,
                               HttpServletRequest request,
                               HttpSession httpSession) {


        String dbCode = shopCustomerService.findByPhone(phone);

        if (!code.equals(dbCode)) {
            throw new ShopException(ResultEnum.CHECK_CODE_FAILE);
        }

        // 验证成功,把手机号写入数据库
        ShopCustomerEntity shopCustomerEntity = shopCustomerService.
                findByOpenid(openid);
        shopCustomerEntity.setPhone(phone);
        shopCustomerService.save(shopCustomerEntity);

        //绑定手机号成功, 把 token 和 openid 放入 Cookie 中
        TokenAuthenticationService.addToken2Cookie(request, response, openid);
        return ShopResult.success();
    }

    /**
     * 获取用户信息
     *
     * @param openid
     * @return
     */
    @ApiOperation(value = "获取用户信息", notes = "通过openid获取用户信息,")
    @RequestMapping(value = "/getuser/{openid}", method = RequestMethod.GET)
    @LoggerManage(logDescription = "获取用户信息")
    public ShopResult<UserVO> getUserByOpenid(@PathVariable(name = "openid") String openid,
                                              HttpServletRequest request) {

        //通过opneid 查找
        ShopCustomerEntity shopCustomerEntity = shopCustomerService.findByOpenid(openid);
        //得到 商城基本配置信息
        ShopConfigEntity shopConfigEntity = shopCustomerService.getShopConfigEntity();
        //通过openid查找 ShopCrm表数据,注意首次注册时查不到的情况
        ShopCrmEntity shopCrmEntity = shopCrmRepository.findByOpenid(shopCustomerEntity.getOpenid());
        if(shopCrmEntity==null){

             shopCrmEntity = new ShopCrmEntity();
             shopCrmEntity.setVipClass("普通会员");

        }

        UserVO userVO = new UserVO(shopCustomerEntity.getPhone(),
                shopCustomerEntity.getUsername(),
                shopCustomerEntity.getHeadImage(),
                shopCrmEntity.getVipClass(),
                shopCustomerEntity.getPoint(),
                shopConfigEntity.getCardUrl(),
                shopConfigEntity.getVipExplain());

        return ShopResult.success(userVO);
    }


    @ApiOperation(value = "完善用户信息", notes = "完善用户信息")
    @RequestMapping(value = "/info/{openid}", method = RequestMethod.POST)
    @LoggerManage(logDescription = "完善用户信息")
    public ShopResult info(@PathVariable(value = "openid") String openid, UserSupplementVO userSupplementVO) {

        shopCustomerService.info(openid, userSupplementVO);
        return ShopResult.success();
    }

    @ApiOperation(value = "获取用户表信息", notes = "通过openid获取用户表信息")
    @RequestMapping(value = "/getUserInfo/{openid}", method = RequestMethod.GET)
    @LoggerManage(logDescription = "获取用户表信息")
    public ShopResult<ShopCustomerEntity> getOne(@PathVariable("openid") String openid) {

        ShopCustomerEntity customerEntity = shopCustomerService.findByOpenid(openid);

        return ShopResult.success(customerEntity);
    }

    /**
     * 通过手机号获取 淘宝,京东账号信息
     *
     * @param mobile
     * @return
     */
    @ApiOperation(value = "关联 淘宝,京东账号信息", notes = "通过手机号获取 淘宝,京东账号信息")
    @RequestMapping(value = "/account/{mobile}", method = RequestMethod.GET)
    @LoggerManage(logDescription = "关联 淘宝,京东账号信息")
    public ShopResult<Map<String, List<String>>> getAccountByMobile(@ApiParam("手机号") @PathVariable(value = "mobile") String mobile) {

        Map<String, List<String>> map = shopCustomerService.getAccountByMobile(mobile);



        return ShopResult.success(map);
    }

    @ApiOperation(value = "绑定淘宝京东账号", notes = "绑定淘宝京东账号")
    @RequestMapping(value = "/bind/{openid}", method = RequestMethod.POST)
    @LoggerManage(logDescription = "绑定淘宝京东账号")
    public ShopResult bindAccount(@PathVariable(value = "openid") String openid,
                                  @ApiParam("账户类型(taobao或者jd)") @RequestParam(value = "type") String type,
                                  @ApiParam("账号") @RequestParam(value = "account") String account) {

        shopCustomerService.bind(openid, type, account);

        return ShopResult.success();
    }


}
