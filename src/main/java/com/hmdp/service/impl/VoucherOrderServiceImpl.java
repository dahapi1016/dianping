package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherOrderDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdUtil;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdUtil redisIdUtil;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckillQualificationVerify.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //判断是否符合秒杀条件（时间、库存）
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始！");
        }

        if(LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已经结束！");
        }

        if(seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                UserHolder.getUser().getId().toString(), voucherId.toString()
        );

        int res = (result != null) ? result.intValue() : 0;

        if(res != 0) {
            return Result.fail(res == 1 ? "库存不足！" : "不能重复下单！");
        }
        VoucherOrderDTO dto = new VoucherOrderDTO();
        dto.setUserId(UserHolder.getUser().getId());
        dto.setVoucherId(voucherId);
        rabbitTemplate.convertAndSend("order.topic", "order.success", dto);
        return Result.ok(voucherId);
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId, Long userId) {
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdUtil.nextId("voucher_order"));
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        seckillVoucherService.update()
                        .setSql("stock = stock - 1")
                        .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0)
                        .update();
        save(voucherOrder);
        return Result.ok(voucherId);
    }
}
