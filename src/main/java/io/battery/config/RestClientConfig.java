package io.battery.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.config.HypermediaRestTemplateConfigurer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import io.battery.ProfileNames;
import io.battery.web.HypermediaClient;

@Configuration
@Profile(value = ProfileNames.ONLINE)
public class RestClientConfig implements RestTemplateCustomizer {
    @Value("${battery.http.maxTotal:0}")
    private int maxTotal;

    @Value("${battery.http.maxConnPerRoute:0}")
    private int maxConnPerRoute;

    @Autowired
    private HypermediaRestTemplateConfigurer hypermediaConfigurer;

    @Override
    public void customize(RestTemplate restTemplate) {
        if (maxConnPerRoute <= 0 || maxTotal <= 0) {
            maxConnPerRoute = Runtime.getRuntime().availableProcessors() * 8;
            maxTotal = maxConnPerRoute * 2;
        }

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMinutes(1))
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setMaxConnTotal(maxTotal)
                .setMaxConnPerRoute(maxConnPerRoute)
                .build();

        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

        hypermediaConfigurer.registerHypermediaTypes(restTemplate);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public HypermediaClient hypermediaClient(RestTemplate template) {
        return new HypermediaClient(template);
    }
}
