package az.blacklist.person.verification.config;

import org.apache.lucene.search.spell.LevenshteinDistance;
import org.apache.lucene.search.spell.JaroWinklerDistance;

import org.apache.lucene.search.spell.StringDistance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StringDistanceConfig {
    @Bean
    public StringDistance stringDistance() {
        return new LevenshteinDistance();
    	//return new JaroWinklerDistance();
    }
}
