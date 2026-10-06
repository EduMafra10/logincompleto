package br.com.loginseguro.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("verificar-conexao")
public class VerificadorConexaoMongo implements CommandLineRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(VerificadorConexaoMongo.class);

    private final MongoTemplate mongoTemplate;

    public VerificadorConexaoMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        mongoTemplate.executeCommand(new Document("ping", 1));
        logger.info("Conexão com o MongoDB confirmada.");
    }
}