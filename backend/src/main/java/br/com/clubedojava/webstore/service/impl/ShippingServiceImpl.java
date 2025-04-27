package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.ShippingOptionDTO;
import br.com.clubedojava.webstore.exception.ShippingException;
import br.com.clubedojava.webstore.model.Order;
import br.com.clubedojava.webstore.service.ShippingService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shipping.correios.api.url}")
    private String correiosApiUrl;

    @Value("${shipping.correios.api.key}")
    private String correiosApiKey;

    @Value("${shipping.jadlog.api.url}")
    private String jadlogApiUrl;

    @Value("${shipping.jadlog.api.key}")
    private String jadlogApiKey;

    @Value("${shipping.braspress.api.url}")
    private String braspressApiUrl;

    @Value("${shipping.braspress.api.key}")
    private String braspressApiKey;

    @Value("${shipping.origin.zipcode}")
    private String originZipCode;

    @Value("${shipping.origin.city}")
    private String originCity;

    @Value("${shipping.origin.state}")
    private String originState;

    @Override
    @Suspendable
    public List<ShippingOptionDTO> calculateShippingOptions(String zipCodeOrigin, String zipCodeDestination, double weight, double length, double width, double height) {
        List<ShippingOptionDTO> allOptions = new ArrayList<>();

        try {
            // Executa requisições em paralelo usando CompletableFuture
            CompletableFuture<List<ShippingOptionDTO>> correiosFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return calculateCorreiosShipping(zipCodeOrigin, zipCodeDestination, weight, length, width, height);
                } catch (Exception e) {
                    log.error("Error calculating Correios shipping", e);
                    return List.of();
                }
            });

            CompletableFuture<List<ShippingOptionDTO>> jadlogFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return calculateJadlogShipping(zipCodeOrigin, zipCodeDestination, weight, length, width, height);
                } catch (Exception e) {
                    log.error("Error calculating Jadlog shipping", e);
                    return List.of();
                }
            });

            CompletableFuture<List<ShippingOptionDTO>> braspressFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return calculateBraspressShipping(zipCodeOrigin, zipCodeDestination, weight, length, width, height);
                } catch (Exception e) {
                    log.error("Error calculating Braspress shipping", e);
                    return List.of();
                }
            });

            CompletableFuture.allOf(correiosFuture, jadlogFuture, braspressFuture).join();

            allOptions.addAll(correiosFuture.join());
            allOptions.addAll(jadlogFuture.join());
            allOptions.addAll(braspressFuture.join());

            // Ordena as opções por preço
            return allOptions.stream()
                    .sorted(Comparator.comparing(ShippingOptionDTO::getPrice))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error calculating shipping options", e);
            throw new ShippingException("Failed to calculate shipping options: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    public BigDecimal calculateShipping(String shippingMethod, String zipCodeDestination, double weight) {
        try {
            // Formato padrão: CARRIER_CODE
            String[] parts = shippingMethod.split("_");
            String carrier = parts[0];
            String code = parts.length > 1 ? parts[1] : "";

            // Dimensões padrão para uma caixa média
            double length = 30.0;
            double width = 20.0;
            double height = 15.0;

            List<ShippingOptionDTO> options;

            switch (carrier.toUpperCase()) {
                case "CORREIOS":
                    options = calculateCorreiosShipping(originZipCode, zipCodeDestination, weight, length, width, height);
                    break;
                case "JADLOG":
                    options = calculateJadlogShipping(originZipCode, zipCodeDestination, weight, length, width, height);
                    break;
                case "BRASPRESS":
                    options = calculateBraspressShipping(originZipCode, zipCodeDestination, weight, length, width, height);
                    break;
                default:
                    throw new ShippingException("Unsupported carrier: " + carrier);
            }

            return options.stream()
                    .filter(option -> option.getCode().equalsIgnoreCase(code))
                    .findFirst()
                    .orElseThrow(() -> new ShippingException("Shipping method not found: " + shippingMethod))
                    .getPrice();
        } catch (Exception e) {
            log.error("Error calculating shipping cost", e);
            throw new ShippingException("Failed to calculate shipping cost: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    public String generateShippingLabel(Order order, String shippingMethod) {
        try {
            String[] parts = shippingMethod.split("_");
            String carrier = parts[0];

            switch (carrier.toUpperCase()) {
                case "CORREIOS":
                    return generateCorreiosLabel(order);
                case "JADLOG":
                    return generateJadlogLabel(order);
                case "BRASPRESS":
                    return generateBraspressLabel(order);
                default:
                    throw new ShippingException("Unsupported carrier for label generation: " + carrier);
            }
        } catch (Exception e) {
            log.error("Error generating shipping label", e);
            throw new ShippingException("Failed to generate shipping label: " + e.getMessage(), e);
        }
    }

    @Override
    @Suspendable
    public String getShippingTracking(String trackingNumber, String shippingMethod) {
        try {
            String[] parts = shippingMethod.split("_");
            String carrier = parts[0];

            switch (carrier.toUpperCase()) {
                case "CORREIOS":
                    return getCorreiosTracking(trackingNumber);
                case "JADLOG":
                    return getJadlogTracking(trackingNumber);
                case "BRASPRESS":
                    return getBraspressTracking(trackingNumber);
                default:
                    throw new ShippingException("Unsupported carrier for tracking: " + carrier);
            }
        } catch (Exception e) {
            log.error("Error getting shipping tracking", e);
            throw new ShippingException("Failed to get shipping tracking: " + e.getMessage(), e);
        }
    }

    // IMPLEMENTAÇÕES DOS MÉTODOS DE CÁLCULO DE FRETE PARA CADA TRANSPORTADORA

    @Suspendable
    private List<ShippingOptionDTO> calculateCorreiosShipping(String zipCodeOrigin, String zipCodeDestination, double weight, double length, double width, double height) {
        try {
            List<ShippingOptionDTO> options = new ArrayList<>();

            // Formata os CEPs removendo caracteres não numéricos
            zipCodeOrigin = zipCodeOrigin.replaceAll("[^0-9]", "");
            zipCodeDestination = zipCodeDestination.replaceAll("[^0-9]", "");

            // Prepara a requisição para a API dos Correios
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + correiosApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cepOrigem", zipCodeOrigin);
            requestBody.put("cepDestino", zipCodeDestination);
            requestBody.put("peso", weight);
            requestBody.put("comprimento", length);
            requestBody.put("largura", width);
            requestBody.put("altura", height);

            // Adiciona serviços dos Correios que queremos consultar
            requestBody.put("servicos", List.of("04510", "04014")); // SEDEX e PAC

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    correiosApiUrl + "/preco/prazo",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode resultados = jsonResponse.get("resultados");

                if (resultados != null && resultados.isArray()) {
                    for (JsonNode resultado : resultados) {
                        String code = resultado.get("codigo").asText();
                        String name = getCorreiosServiceName(code);
                        BigDecimal price = new BigDecimal(resultado.get("valor").asText().replace(",", "."));
                        int deliveryTime = resultado.get("prazoEntrega").asInt();

                        LocalDate estimatedDelivery = LocalDate.now().plusDays(deliveryTime);

                        options.add(ShippingOptionDTO.builder()
                                .code(code)
                                .name(name)
                                .carrier("CORREIOS")
                                .price(price)
                                .deliveryTimeInDays(deliveryTime)
                                .estimatedDelivery(estimatedDelivery)
                                .build());
                    }
                }
            }

            return options;
        } catch (Exception e) {
            log.error("Error calculating Correios shipping", e);
            throw new ShippingException("Failed to calculate Correios shipping: " + e.getMessage(), e);
        }
    }

    @Suspendable
    private List<ShippingOptionDTO> calculateJadlogShipping(String zipCodeOrigin, String zipCodeDestination, double weight, double length, double width, double height) {
        try {
            List<ShippingOptionDTO> options = new ArrayList<>();

            // Formata os CEPs removendo caracteres não numéricos
            zipCodeOrigin = zipCodeOrigin.replaceAll("[^0-9]", "");
            zipCodeDestination = zipCodeDestination.replaceAll("[^0-9]", "");

            // Prepara a requisição para a API da Jadlog
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jadlogApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cepOrigem", zipCodeOrigin);
            requestBody.put("cepDestino", zipCodeDestination);
            requestBody.put("peso", weight);
            requestBody.put("valorDeclarado", 0); // Valor declarado (opcional)

            // Adiciona modalidades da Jadlog que queremos consultar
            List<Map<String, Object>> modalidades = new ArrayList<>();
            Map<String, Object> modalidade1 = new HashMap<>();
            modalidade1.put("codigo", "0"); // Expresso
            modalidades.add(modalidade1);

            Map<String, Object> modalidade2 = new HashMap<>();
            modalidade2.put("codigo", "4"); // Rodoviário
            modalidades.add(modalidade2);

            requestBody.put("modalidades", modalidades);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    jadlogApiUrl + "/frete/consulta",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode retorno = jsonResponse.get("retorno");

                if (retorno != null && retorno.get("modalidades").isArray()) {
                    for (JsonNode modalidade : retorno.get("modalidades")) {
                        String code = modalidade.get("codigo").asText();
                        String name = getJadlogServiceName(code);
                        BigDecimal price = new BigDecimal(modalidade.get("valor").asText().replace(",", "."));
                        int deliveryTime = modalidade.get("prazo").asInt();

                        LocalDate estimatedDelivery = LocalDate.now().plusDays(deliveryTime);

                        options.add(ShippingOptionDTO.builder()
                                .code(code)
                                .name(name)
                                .carrier("JADLOG")
                                .price(price)
                                .deliveryTimeInDays(deliveryTime)
                                .estimatedDelivery(estimatedDelivery)
                                .build());
                    }
                }
            }

            return options;
        } catch (Exception e) {
            log.error("Error calculating Jadlog shipping", e);
            throw new ShippingException("Failed to calculate Jadlog shipping: " + e.getMessage(), e);
        }
    }

    @Suspendable
    private List<ShippingOptionDTO> calculateBraspressShipping(String zipCodeOrigin, String zipCodeDestination, double weight, double length, double width, double height) {
        try {
            List<ShippingOptionDTO> options = new ArrayList<>();

            // Formata os CEPs removendo caracteres não numéricos
            zipCodeOrigin = zipCodeOrigin.replaceAll("[^0-9]", "");
            zipCodeDestination = zipCodeDestination.replaceAll("[^0-9]", "");

            // Prepara a requisição para a API da Braspress
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + braspressApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cepOrigem", zipCodeOrigin);
            requestBody.put("cidadeOrigem", originCity);
            requestBody.put("estadoOrigem", originState);
            requestBody.put("cepDestino", zipCodeDestination);
            requestBody.put("peso", weight);
            requestBody.put("valorMercadoria", 0);

            // Dimensões (em metros para Braspress)
            requestBody.put("comprimento", length / 100);
            requestBody.put("largura", width / 100);
            requestBody.put("altura", height / 100);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    braspressApiUrl + "/cotacao",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                if (jsonResponse.has("valorFrete") && jsonResponse.has("prazoEntrega")) {
                    BigDecimal price = new BigDecimal(jsonResponse.get("valorFrete").asText().replace(",", "."));
                    int deliveryTime = jsonResponse.get("prazoEntrega").asInt();

                    LocalDate estimatedDelivery = LocalDate.now().plusDays(deliveryTime);

                    options.add(ShippingOptionDTO.builder()
                            .code("STD")
                            .name("Braspress Standard")
                            .carrier("BRASPRESS")
                            .price(price)
                            .deliveryTimeInDays(deliveryTime)
                            .estimatedDelivery(estimatedDelivery)
                            .build());
                }
            }

            return options;
        } catch (Exception e) {
            log.error("Error calculating Braspress shipping", e);
            throw new ShippingException("Failed to calculate Braspress shipping: " + e.getMessage(), e);
        }
    }

    // MÉTODOS PARA GERAÇÃO DE ETIQUETAS

    private String generateCorreiosLabel(Order order) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + correiosApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            // Dados do remetente
            Map<String, Object> sender = new HashMap<>();
            sender.put("nome", "Clube do Java");
            sender.put("cep", originZipCode);
            sender.put("telefone", "1199999999");
            requestBody.put("remetente", sender);

            // Dados do destinatário
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("nome", order.getUser().getName());
            recipient.put("documento", order.getUser().getCpfCnpj());
            recipient.put("cep", order.getShippingAddress().getZipCode());
            recipient.put("endereco", order.getShippingAddress().getStreet());
            recipient.put("numero", order.getShippingAddress().getNumber());
            recipient.put("complemento", order.getShippingAddress().getComplement());
            recipient.put("bairro", order.getShippingAddress().getNeighborhood());
            recipient.put("cidade", order.getShippingAddress().getCity());
            recipient.put("uf", order.getShippingAddress().getState());
            recipient.put("telefone", order.getUser().getPhone());
            requestBody.put("destinatario", recipient);

            // Dados da encomenda
            Map<String, Object> package_ = new HashMap<>();
            package_.put("servico", "04510"); // SEDEX
            package_.put("codigoRastreamento", "");
            package_.put("peso", order.getItems().stream().mapToDouble(i -> i.getProduct().getWeight() * i.getQuantity()).sum());
            package_.put("valorDeclarado", order.getTotalAmount());
            requestBody.put("encomenda", package_);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    correiosApiUrl + "/etiquetas",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("codigoRastreamento").asText();
            } else {
                throw new ShippingException("Failed to generate Correios label: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Error generating Correios label", e);
            throw new ShippingException("Failed to generate Correios label: " + e.getMessage(), e);
        }
    }

    private String generateJadlogLabel(Order order) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jadlogApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            // Configuração semelhante à dos Correios, mas adaptada para a API da Jadlog

            // Simulação - em produção, integrar com a API real da Jadlog
            return "JL" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } catch (Exception e) {
            log.error("Error generating Jadlog label", e);
            throw new ShippingException("Failed to generate Jadlog label: " + e.getMessage(), e);
        }
    }

    private String generateBraspressLabel(Order order) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + braspressApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            // Configuração semelhante, mas adaptada para a API da Braspress

            // Simulação - em produção, integrar com a API real da Braspress
            return "BP" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } catch (Exception e) {
            log.error("Error generating Braspress label", e);
            throw new ShippingException("Failed to generate Braspress label: " + e.getMessage(), e);
        }
    }

    // MÉTODOS PARA CONSULTA DE RASTREAMENTO

    private String getCorreiosTracking(String trackingNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + correiosApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    correiosApiUrl + "/rastreamento/" + trackingNumber,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ShippingException("Failed to get Correios tracking: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Error getting Correios tracking", e);
            throw new ShippingException("Failed to get Correios tracking: " + e.getMessage(), e);
        }
    }

    private String getJadlogTracking(String trackingNumber) {
        // Implementação semelhante à dos Correios, mas adaptada para a API da Jadlog
        return "Tracking information for " + trackingNumber;
    }

    private String getBraspressTracking(String trackingNumber) {
        // Implementação semelhante, mas adaptada para a API da Braspress
        return "Tracking information for " + trackingNumber;
    }

    // MÉTODOS AUXILIARES

    private String getCorreiosServiceName(String code) {
        return switch (code) {
            case "04510" -> "SEDEX";
            case "04014" -> "PAC";
            case "04782" -> "SEDEX 12";
            case "04790" -> "SEDEX 10";
            case "04804" -> "SEDEX Hoje";
            default -> "Serviço Correios " + code;
        };
    }

    private String getJadlogServiceName(String code) {
        return switch (code) {
            case "0" -> "Jadlog Expresso";
            case "4" -> "Jadlog Rodoviário";
            case "6" -> "Jadlog Econômico";
            case "9" -> "Jadlog Doc";
            case "10" -> "Jadlog Corporate";
            default -> "Serviço Jadlog " + code;
        };
    }
}