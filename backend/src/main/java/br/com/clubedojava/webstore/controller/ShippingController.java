package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.ShippingCalculationRequestDTO;
import br.com.clubedojava.webstore.dto.ShippingOptionDTO;
import br.com.clubedojava.webstore.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/calculate")
    public CompletableFuture<ResponseEntity<List<ShippingOptionDTO>>> calculateShipping(
            @Valid @RequestBody ShippingCalculationRequestDTO requestDTO) {

        return CompletableFuture.supplyAsync(() -> {
            List<ShippingOptionDTO> options = shippingService.calculateShippingOptions(
                    requestDTO.getZipCodeOrigin(),
                    requestDTO.getZipCodeDestination(),
                    requestDTO.getWeight(),
                    requestDTO.getLength(),
                    requestDTO.getWidth(),
                    requestDTO.getHeight());

            return ResponseEntity.ok(options);
        });
    }

    @GetMapping("/tracking/{trackingNumber}")
    public CompletableFuture<ResponseEntity<String>> getTracking(
            @PathVariable String trackingNumber,
            @RequestParam String carrier) {

        return CompletableFuture.supplyAsync(() -> {
            String trackingInfo = shippingService.getShippingTracking(trackingNumber, carrier);
            return ResponseEntity.ok(trackingInfo);
        });
    }
}