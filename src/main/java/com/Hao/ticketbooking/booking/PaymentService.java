package com.Hao.ticketbooking.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A mock payment: no real provider is called. It logs the charge and, if payment.failure-rate
 * is above 0, declines that share of payments at random. The failures exist to practice
 * rollbacks: a declined payment inside the booking transaction must leave nothing behind.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final double failureRate;

    public PaymentService(@Value("${payment.failure-rate}") double failureRate) {
        this.failureRate = failureRate;
    }

    public void charge(Long userId, int amountCents) {
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.info("Mock payment DECLINED: user {} for {} cents", userId, amountCents);
            throw new PaymentFailedException();
        }
        log.info("Mock payment accepted: user {} charged {} cents", userId, amountCents);
    }
}
