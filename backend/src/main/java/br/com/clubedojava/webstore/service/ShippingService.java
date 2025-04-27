package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.ShippingOptionDTO;
import br.com.clubedojava.webstore.model.Order;
import br.com.clubedojava.webstore.service.impl.Suspendable;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ShippingService {

    @Suspendable
    List<ShippingOptionDTO> calculateShippingOptions(String zipCodeOrigin, String zipCodeDestination, double weight, double length, double width, double height);

    @Suspendable
    BigDecimal calculateShipping(String shippingMethod, String zipCodeDestination, double weight);

    @Suspendable
    String generateShippingLabel(Order order, String shippingMethod);

    @Suspendable
    String getShippingTracking(String trackingNumber, String shippingMethod);
}
