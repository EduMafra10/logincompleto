package br.com.loginseguro.seguranca;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VerificacaoSessaoFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(VerificacaoSessaoFilter.class);

    private final VerificacaoSessaoService verificacaoSessaoService;

    private final SecurityContextLogoutHandler encerramento =
            new SecurityContextLogoutHandler();

    public VerificacaoSessaoFilter(
            VerificacaoSessaoService verificacaoSessaoService) {

        this.verificacaoSessaoService = verificacaoSessaoService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeia)
            throws ServletException, IOException {

        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {

            cadeia.doFilter(requisicao, resposta);
            return;
        }

        boolean sessaoValida;

        try {
            sessaoValida =
                    verificacaoSessaoService.estaValida(autenticacao);

        } catch (DataAccessException ex) {
            log.warn("Falha ao consultar as permissões da sessão.", ex);

            resposta.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Não foi possível verificar seu acesso. "
                    + "Tente novamente em instantes."
            );

            return;
        }

        if (!sessaoValida) {
            encerramento.logout(requisicao, resposta, autenticacao);

            resposta.sendRedirect(
                    requisicao.getContextPath()
                    + "/login?sessao=atualizada"
            );

            return;
        }

        cadeia.doFilter(requisicao, resposta);
    }
}