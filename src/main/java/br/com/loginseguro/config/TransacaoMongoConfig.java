package br.com.loginseguro.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransacaoMongoConfig {

    @Bean
    public MongoTransactionManager transactionManager(
            MongoDatabaseFactory databaseFactory) {

        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public InitializingBean prepararControleAdministracao(
            MongoTemplate mongoTemplate) {

        return () -> mongoTemplate.upsert(
                Query.query(Criteria.where("_id").is("edicao-usuarios")),
                new Update().setOnInsert("versao", 0L),
                "controle_administracao"
        );
    }
}