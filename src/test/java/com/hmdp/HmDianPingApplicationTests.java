package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    ShopServiceImpl shopService;

    @Test
    public void testSaveShop() {
        shopService.saveShopToRedis(1L, 30L);
    }
}
