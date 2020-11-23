package com.sibyg.mtls_demo;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

@SpringBootApplication
public class ConsumingRestApplication {

    private static final Logger log = LoggerFactory.getLogger(ConsumingRestApplication.class);
    private final Integer maxConnTotal = Integer.valueOf(5);
    private final Integer maxConnPerRoute = Integer.valueOf(5);
    private final Integer readTimeout = Integer.valueOf(10000);
    private final Integer connectTimeout = Integer.valueOf(10000);
    private final String KEY_STORE_PASSWORD = "secret";
    private final String KEY_PASSWORD = "secret";

    public static void main(String[] args) {
        SpringApplication.run(ConsumingRestApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        KeyStore keyStore;
        HttpComponentsClientHttpRequestFactory requestFactory = null;

        try {
            keyStore = KeyStore.getInstance("jks");
            ClassPathResource classPathResource = new ClassPathResource("keystore.jks");
            try (InputStream in = new FileInputStream(new File("client/src/main/resources/keystore.jks"))) {
                keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
            }

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(new SSLContextBuilder()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .loadKeyMaterial(keyStore, KEY_PASSWORD.toCharArray()).build(),
                    NoopHostnameVerifier.INSTANCE);

            HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
                    .setMaxConnTotal(maxConnTotal)
                    .setMaxConnPerRoute(maxConnPerRoute)
                    .build();

            requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(readTimeout);
            requestFactory.setConnectTimeout(connectTimeout);

            restTemplate.setRequestFactory(requestFactory);
        } catch (Exception exception) {
            System.out.println("Exception Occurred while creating restTemplate " + exception);
            exception.printStackTrace();
        }
        return restTemplate;
    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            String httpsUrl = "https://localhost:8443/saas-gw-ms/api-1";
            String response = restTemplate.getForObject(httpsUrl, String.class);
//            String httpUrl = "http://localhost:8080/saas-gw-ms/api-1";
//            String response = restTemplate.getForObject(httpUrl, String.class);
            log.info(response);
        };
    }
}