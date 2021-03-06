package com.yearcon.pointshop.crm;

import com.yearcon.pointshop.moudles.crm.entity.ShopVipClassConfigEntity;
import com.yearcon.pointshop.moudles.crm.service.ShopVipClassConfigService;
import com.yearcon.pointshop.moudles.schedule.service.CrmTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author itguang
 * @create 2018-01-20 13:39
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TaskTest {

    @Autowired
    ShopVipClassConfigService shopVipClassConfigService;

    @Autowired
    CrmTask crmTask;


    @Test
    public void tetst(){
        List<ShopVipClassConfigEntity> classConfigEntityList = shopVipClassConfigService.getConfig();

        int point = crmTask.getPointByConfig(classConfigEntityList, 300.0);

        log.info("point={}",point);

    }


    @Test
    public void getVipVlassByConfig(){
        List<ShopVipClassConfigEntity> classConfigEntityList = shopVipClassConfigService.getConfig();

        String className = crmTask.getVipVlassByConfig(classConfigEntityList, 300.0);

        log.info("point={}",className);

    }



    @Test
    public void taobao(){

        crmTask.taobao();


    }


    @Test
    public void jd(){

        crmTask.jd();


    }


}
