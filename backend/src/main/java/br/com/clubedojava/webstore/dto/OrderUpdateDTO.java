package br.com.clubedojava.webstore.dto;

import br.com.clubedojava.webstore.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateDTO {
    private OrderStatus status;
    private String trackingNumber;
}