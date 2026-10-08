package br.com.loginseguro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import br.com.loginseguro.seguranca.VerificacaoSessaoFilter;
import br.com.loginseguro.seguranca.VerificacaoSessaoService;
import br.com.loginseguro.usuario.Perfil;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            VerificacaoSessaoService verificacaoSessaoService)
            throws Exception {

        http
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD)
                        .permitAll()

                        .requestMatchers(
                                "/",
                                "/login",
                                "/cadastro",
                                "/css/**",
                                "/js/**",
                                "/imagens/**",
                                "/favicon.ico")
                        .permitAll()

                        .requestMatchers("/admin", "/admin/**")
                        .hasRole(Perfil.ADMINISTRADOR.name())

                        .requestMatchers("/moderacao", "/moderacao/**")
                        .hasAnyRole(
                                Perfil.MODERADOR.name(),
                                Perfil.ADMINISTRADOR.name())

                        .anyRequest()
                        .authenticated()
                )
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/inicio", true)
                        .failureUrl("/login?erro")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        http.addFilterBefore(
                new VerificacaoSessaoFilter(verificacaoSessaoService),
                AuthorizationFilter.class
        );

        return http.build();
    }
}