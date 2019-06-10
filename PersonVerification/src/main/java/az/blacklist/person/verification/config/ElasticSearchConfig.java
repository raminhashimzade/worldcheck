package az.blacklist.person.verification.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    private String host;
    private int port;

    public ElasticSearchConfig(@Value("${elasticsearch.host}") String host,
                               @Value("${elasticsearch.port}") int port) {
        this.host = host;
        this.port = port;
    }

    @Bean
    public RestHighLevelClient client() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port)));
    }
}