package br.com.clubedojava.webstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDTO shippingAddress;

    @NotNull(message = "Billing address is required")
    @Valid
    private AddressDTO billingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Shipping method is required")
    private String shippingMethod;
}