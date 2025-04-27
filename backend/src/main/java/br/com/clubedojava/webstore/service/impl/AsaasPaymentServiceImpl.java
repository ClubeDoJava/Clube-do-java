package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.exception.PaymentException;
import br.com.clubedojava.webstore.model.Order;
import br.com.clubedojava.webstore.model.OrderItem;
import br.com.clubedojava.webstore.model.OrderStatus;
import br.com.clubedojava.webstore.repository.OrderRepository;
import br.com.clubedojava.webstore.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasPaymentServiceImpl implements PaymentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${asaas.api.key}")
    private String asaasApiKey;

    @Value("${asaas.api.url}")
    private String asaasApiUrl;

    @Value("${asaas.webhook.secret}")
    private String asaasWebhookSecret;

    @Override
    @Suspendable
    @Transactional
    public String createPixPayment(Order order) {
        try {
            Map<String, Object> paymentRequest = buildBasePaymentRequest(order);
            paymentRequest.put("billingType", "PIX");

            // Configurações específicas do PIX
            Map<String, Object> pixConfig = new HashMap<>();
            pixConfig.put("expirationDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE));
            pixConfig.put("expirationTime", 60); // 60 minutos
            paymentRequest.put("pixConfig", pixConfig);

            String paymentId = executePaymentRequest(paymentRequest);

            // Atualiza o status da ordem
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);

            return paymentId;
        } catch (Exception e) {
            log.error("Error creating PIX payment", e);
            throw new PaymentException("Failed to create PIX payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    @Transactional
    public String createBoletoPayment(Order order) {
        try {
            Map<String, Object> paymentRequest = buildBasePaymentRequest(order);
            paymentRequest.put("billingType", "BOLETO");

            // Configurações específicas do Boleto
            paymentRequest.put("dueDate", LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_DATE));
            paymentRequest.put("description", "Pedido #" + order.getOrderNumber());

            String paymentId = executePaymentRequest(paymentRequest);

            // Atualiza o status da ordem
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);

            return paymentId;
        } catch (Exception e) {
            log.error("Error creating Boleto payment", e);
            throw new PaymentException("Failed to create Boleto payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    @Transactional
    public String createCreditCardPayment(Order order) {
        // Na implementação real, você receberia os dados do cartão do cliente
        // Isso é apenas uma demonstração
        try {
            Map<String, Object> paymentRequest = buildBasePaymentRequest(order);
            paymentRequest.put("billingType", "CREDIT_CARD");

            // Dados do cartão de crédito (em produção, esses dados viriam tokenizados do frontend)
            Map<String, Object> creditCard = new HashMap<>();
            // Em produção, nunca armazene dados sensíveis do cartão
            // Use tokenização ou checkout direto da Asaas

            paymentRequest.put("creditCard", creditCard);
            paymentRequest.put("creditCardHolderInfo", buildHolderInfo(order));

            String paymentId = executePaymentRequest(paymentRequest);

            // Atualiza o status da ordem
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);

            return paymentId;
        } catch (Exception e) {
            log.error("Error creating Credit Card payment", e);
            throw new PaymentException("Failed to create Credit Card payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    public boolean verifyPaymentStatus(String paymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/payments/" + paymentId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String status = jsonResponse.get("status").asText();

                // CONFIRMED, RECEIVED ou RECEIVED_IN_CASH indicam pagamento confirmado
                return "CONFIRMED".equals(status) || "RECEIVED".equals(status) || "RECEIVED_IN_CASH".equals(status);
            }

            return false;
        } catch (Exception e) {
            log.error("Error verifying payment status", e);
            return false;
        }
    }

    @Override
    @Suspendable
    @Transactional
    public void refundPayment(String paymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/payments/" + paymentId + "/refund",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new PaymentException("Failed to refund payment: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Error refunding payment", e);
            throw new PaymentException("Failed to refund payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    @Transactional
    public void handleAsaasWebhook(String payload, String signature) {
        try {
            // Verifica a assinatura do webhook
            if (!verifyWebhookSignature(payload, signature)) {
                log.error("Invalid webhook signature");
                throw new PaymentException("Invalid webhook signature");
            }

            JsonNode jsonPayload = objectMapper.readTree(payload);
            String event = jsonPayload.get("event").asText();
            JsonNode payment = jsonPayload.get("payment");
            String paymentId = payment.get("id").asText();
            String status = payment.get("status").asText();

            // Busca a ordem pelo paymentIntentId
            Order order = orderRepository.findByPaymentIntentId(paymentId)
                    .orElseThrow(() -> new PaymentException("Order not found for payment: " + paymentId));

            OrderStatus oldStatus = order.getStatus();
            OrderStatus newStatus = oldStatus;

            switch (event) {
                case "PAYMENT_CONFIRMED", "PAYMENT_RECEIVED":
                    newStatus = OrderStatus.PAYMENT_COMPLETED;
                    break;
                case "PAYMENT_OVERDUE":
                    // Pagamento atrasado, manter como PAYMENT_PROCESSING
                    break;
                case "PAYMENT_REFUNDED":
                    newStatus = OrderStatus.REFUNDED;
                    break;
                case "PAYMENT_DELETED", "PAYMENT_FAILED":
                    newStatus = OrderStatus.CANCELLED;
                    break;
                default:
                    log.info("Unhandled Asaas event: {}", event);
            }

            if (newStatus != oldStatus) {
                order.setStatus(newStatus);
                orderRepository.save(order);

                // Publica evento de atualização
                eventPublisher.publishEvent(new Order.OrderUpdatedEvent(
                        order.getId(),
                        oldStatus,
                        newStatus,
                        java.time.LocalDateTime.now()
                ));
            }
        } catch (Exception e) {
            log.error("Error handling Asaas webhook", e);
            throw new PaymentException("Failed to process Asaas webhook: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildBasePaymentRequest(Order order) {
        Map<String, Object> paymentRequest = new HashMap<>();

        // Dados básicos do pagamento
        paymentRequest.put("customer", getOrCreateAsaasCustomer(order));
        paymentRequest.put("value", order.getTotalAmount());
        paymentRequest.put("externalReference", order.getOrderNumber());

        // Lista de produtos
        paymentRequest.put("items", order.getItems().stream()
                .map(this::mapOrderItemToAsaasItem)
                .collect(Collectors.toList()));

        // Endereço de cobrança
        if (order.getBillingAddress() != null) {
            Map<String, Object> address = new HashMap<>();
            address.put("street", order.getBillingAddress().getStreet());
            address.put("number", order.getBillingAddress().getNumber());
            address.put("complement", order.getBillingAddress().getComplement());
            address.put("neighborhood", order.getBillingAddress().getNeighborhood());
            address.put("city", order.getBillingAddress().getCity());
            address.put("state", order.getBillingAddress().getState());
            address.put("postalCode", order.getBillingAddress().getZipCode().replaceAll("[^0-9]", ""));
            paymentRequest.put("address", address);
        }

        return paymentRequest;
    }

    private String getOrCreateAsaasCustomer(Order order) {
        // Verifica se o cliente já existe na Asaas
        String cpfCnpj = order.getUser().getCpfCnpj();
        if (cpfCnpj == null || cpfCnpj.isEmpty()) {
            throw new PaymentException("User CPF/CNPJ is required for payment");
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Busca cliente por CPF/CNPJ
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/customers?cpfCnpj=" + cpfCnpj,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode data = jsonResponse.get("data");

            if (data.isArray() && data.size() > 0) {
                // Cliente já existe
                return data.get(0).get("id").asText();
            } else {
                // Cria novo cliente
                return createAsaasCustomer(order);
            }
        } catch (Exception e) {
            log.error("Error searching for Asaas customer", e);
            throw new PaymentException("Failed to search for Asaas customer: " + e.getMessage(), e);
        }
    }

    private String createAsaasCustomer(Order order) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> customerRequest = new HashMap<>();
            customerRequest.put("name", order.getUser().getName());
            customerRequest.put("email", order.getUser().getEmail());
            customerRequest.put("phone", order.getUser().getPhone());
            customerRequest.put("cpfCnpj", order.getUser().getCpfCnpj().replaceAll("[^0-9]", ""));

            // Endereço do cliente
            if (order.getShippingAddress() != null) {
                customerRequest.put("postalCode", order.getShippingAddress().getZipCode().replaceAll("[^0-9]", ""));
                customerRequest.put("address", order.getShippingAddress().getStreet());
                customerRequest.put("addressNumber", order.getShippingAddress().getNumber());
                customerRequest.put("complement", order.getShippingAddress().getComplement());
                customerRequest.put("province", order.getShippingAddress().getNeighborhood());
                customerRequest.put("city", order.getShippingAddress().getCity());
                customerRequest.put("state", order.getShippingAddress().getState());
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(customerRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/customers",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("id").asText();
            } else {
                throw new PaymentException("Failed to create Asaas customer: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Error creating Asaas customer", e);
            throw new PaymentException("Failed to create Asaas customer: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mapOrderItemToAsaasItem(OrderItem item) {
        Map<String, Object> asaasItem = new HashMap<>();
        asaasItem.put("description", item.getProduct().getName());
        asaasItem.put("quantity", item.getQuantity());
        asaasItem.put("value", item.getPrice());
        return asaasItem;
    }

    private Map<String, Object> buildHolderInfo(Order order) {
        Map<String, Object> holderInfo = new HashMap<>();
        holderInfo.put("name", order.getUser().getName());
        holderInfo.put("email", order.getUser().getEmail());
        holderInfo.put("cpfCnpj", order.getUser().getCpfCnpj().replaceAll("[^0-9]", ""));
        holderInfo.put("postalCode", order.getBillingAddress().getZipCode().replaceAll("[^0-9]", ""));
        holderInfo.put("addressNumber", order.getBillingAddress().getNumber());
        holderInfo.put("phone", order.getUser().getPhone());
        return holderInfo;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access_token", asaasApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String executePaymentRequest(Map<String, Object> paymentRequest) throws Exception {
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                asaasApiUrl + "/payments",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.get("id").asText();
        } else {
            throw new PaymentException("Failed to create payment: " + response.getBody());
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // Usa BouncyCastle para verificar a assinatura HMAC-SHA256
            byte[] key = asaasWebhookSecret.getBytes(StandardCharsets.UTF_8);
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);

            HMac hmac = new HMac(new SHA256Digest());
            hmac.init(new KeyParameter(key));
            hmac.update(data, 0, data.length);

            byte[] result = new byte[hmac.getMacSize()];
            hmac.doFinal(result, 0);

            String calculatedSignature = Base64.getEncoder().encodeToString(result);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
}