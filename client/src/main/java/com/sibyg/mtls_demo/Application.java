package com.sibyg.mtls_demo;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private final Integer maxConnTotal = Integer.valueOf(5);
    private final Integer maxConnPerRoute = Integer.valueOf(5);
    private final Integer readTimeout = Integer.valueOf(10000);
    private final Integer connectTimeout = Integer.valueOf(10000);
    private final String KEY_STORE_PASSWORD = "secret";
    private final String KEY_PASSWORD = "secret";
    private final String SSL_PATH = "src/main/resources/ssl/";


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        KeyStore keyStore;
        HttpComponentsClientHttpRequestFactory requestFactory = null;

        try {
            keyStore = KeyStore.getInstance("jks");
//            ClassPathResource classPathResource = new ClassPathResource("keystore.jks");
            // TODO replace with classpath resource
            try (InputStream in = new FileInputStream(new File("client/src/main/resources/keystore.jks"))) {
                keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
            }

            SSLConnectionSocketFactory socketFactory;
            socketFactory = new SSLConnectionSocketFactory(new SSLContextBuilder()
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

    private RestTemplate restTemplateWithSSL() throws Exception {

        File clientCer = new File(SSL_PATH + "client-signed.cer");
        File clientPrivateKey = new File(SSL_PATH + "client-private.key");
        File caCer = new File(SSL_PATH + "ca.cer");

        log.info("CURRENT DIR:" + System.getProperty("user.dir")
                + "\n" + clientCer.exists()
                + "\n" + clientPrivateKey.exists()
                + "\n" + caCer.exists());

        String password = "secret";

        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] pwdArray = password.toCharArray();
        ks.load(null, pwdArray);

        X509Certificate[] certificateChain = new X509Certificate[2];


        certificateChain[0] = PemUtil.getx509Certificate(clientCer);
        certificateChain[1] = PemUtil.getx509Certificate(caCer);
        ks.setKeyEntry("sso-signing-key", PemUtil.readPrivateKey(SSL_PATH + "client-private.key"), pwdArray, certificateChain);


        SSLContext sslContext =
                SSLContextBuilder.create()
                        .loadKeyMaterial(ks, password.toCharArray())
                        .loadTrustMaterial(ks, new TrustAllStrategy())
                        .build();

        HttpClient client = HttpClients.custom().setSSLContext(sslContext).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(client);

        return new RestTemplateBuilder().requestFactory(() -> requestFactory).build();
    }


    @Bean
    public CommandLineRunner run() {
        return args -> {
            String httpsUrl = "https://localhost:8443/saas-gw-ms/api-1";
            String response = restTemplateWithSSL().getForObject(httpsUrl, String.class);
//            String httpUrl = "http://localhost:8080/saas-gw-ms/api-1";
//            String response = restTemplate.getForObject(httpUrl, String.class);
            log.info(response);
        };
    }
}