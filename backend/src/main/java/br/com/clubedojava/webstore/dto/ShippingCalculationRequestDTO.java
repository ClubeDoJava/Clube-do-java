package br.com.clubedojava.webstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCalculationRequestDTO {

    @NotBlank(message = "Origin ZIP code is required")
    private String zipCodeOrigin;

    @NotBlank(message = "Destination ZIP code is required")
    private String zipCodeDestination;

    @Positive(message = "Weight must be positive")
    private double weight;

    @Positive(message = "Length must be positive")
    private double length;

    @Positive(message = "Width must be positive")
    private double width;

    @Positive(message = "Height must be positive")
    private double height;
}