package br.com.clubedojava.webstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOptionDTO {
    private String code;
    private String name;
    private String carrier;
    private BigDecimal price;
    private Integer deliveryTimeInDays;
    private LocalDate estimatedDelivery;
}