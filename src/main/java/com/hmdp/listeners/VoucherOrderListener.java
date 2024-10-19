package com.hmdp.listeners;

import com.hmdp.dto.VoucherOrderDTO;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderListener {

    @Resource
    private VoucherOrderServiceImpl voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
                    value = @Queue(name = "order.create.queue", durable = "true"),
                    exchange = @Exchange(name = "order.topic", type = ExchangeTypes.TOPIC),
                    key = "order.success"
            ), ackMode = "AUTO"
    )
    public void listenSecKillVoucherQueue(VoucherOrderDTO dto) {
        voucherOrderService.createVoucherOrder(dto.getVoucherId(), dto.getUserId());
    }
}
