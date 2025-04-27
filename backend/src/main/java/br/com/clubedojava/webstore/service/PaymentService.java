package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.model.Order;
import br.com.clubedojava.webstore.service.impl.Suspendable;

public interface PaymentService {

    @Suspendable
    String createPixPayment(Order order);

    @Suspendable
    String createBoletoPayment(Order order);

    @Suspendable
    String createCreditCardPayment(Order order);

    @Suspendable
    boolean verifyPaymentStatus(String paymentId);

    @Suspendable
    void refundPayment(String paymentId);

    @Suspendable
    void handleAsaasWebhook(String payload, String signature);
}
