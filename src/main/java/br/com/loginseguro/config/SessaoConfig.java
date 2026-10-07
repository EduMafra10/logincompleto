package br.com.loginseguro.config;

import org.mongodb.spring.session.config.annotation.web.http.EnableMongoHttpSession;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMongoHttpSession
public class SessaoConfig {
}