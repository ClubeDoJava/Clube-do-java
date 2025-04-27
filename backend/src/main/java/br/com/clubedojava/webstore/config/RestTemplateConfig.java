package br.com.clubedojava.webstore.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        // Configurar timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15000)
                .setSocketTimeout(30000)
                .build();

        // Construir o HttpClient com as configurações
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        // Configurar o request factory
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        // Criar e retornar o RestTemplate
        return new RestTemplate(requestFactory);
    }
}