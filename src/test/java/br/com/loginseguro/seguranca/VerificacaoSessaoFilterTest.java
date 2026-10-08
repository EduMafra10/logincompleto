package br.com.loginseguro.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.loginseguro.usuario.Perfil;
import br.com.loginseguro.usuario.Usuario;
import br.com.loginseguro.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class VerificacaoSessaoFilterTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain cadeia;

    private VerificacaoSessaoFilter filtro;
    private MockHttpServletRequest requisicao;
    private MockHttpServletResponse resposta;
    private MockHttpSession sessao;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();

        VerificacaoSessaoService service =
                new VerificacaoSessaoService(usuarioRepository);

        filtro = new VerificacaoSessaoFilter(service);

        requisicao = new MockHttpServletRequest("GET", "/admin");
        resposta = new MockHttpServletResponse();
        sessao = new MockHttpSession();

        requisicao.setSession(sessao);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveContinuarQuandoContaEPerfilContinuamValidos()
            throws Exception {

        autenticarAdministrador();
        simularConta(Perfil.ADMINISTRADOR, true);

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        assertEquals(200, resposta.getStatus());
        assertFalse(sessao.isInvalid());

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @CsvSource({
            "MODERADOR, true",
            "ADMINISTRADOR, false"
    })
    void deveEncerrarSessaoQuandoPerfilOuSituacaoMudam(
            Perfil perfil, boolean ativo) throws Exception {

        autenticarAdministrador();
        simularConta(perfil, ativo);

        filtro.doFilter(requisicao, resposta, cadeia);

        verificarEncerramento();
    }

    @Test
    void deveRecusarSessaoComPerfilExtraDesatualizado()
            throws Exception {

        simularConta(Perfil.USUARIO, true);

        var autenticacao =
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin@exemplo.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USUARIO"),
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMINISTRADOR"),
                                FactorGrantedAuthority.fromAuthority(
                                        FactorGrantedAuthority.PASSWORD_AUTHORITY)
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(autenticacao);

        filtro.doFilter(requisicao, resposta, cadeia);

        verificarEncerramento();
    }

    @Test
    void deveEncerrarSessaoQuandoContaNaoExisteMais()
            throws Exception {

        autenticarAdministrador();

        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenReturn(Optional.empty());

        filtro.doFilter(requisicao, resposta, cadeia);

        verificarEncerramento();
    }

    @Test
    void deveContinuarSemConsultarBancoQuandoNaoHaAutenticacao()
            throws Exception {

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveContinuarSemConsultarBancoParaVisitanteAnonimo()
            throws Exception {

        var autenticacao = new AnonymousAuthenticationToken(
                "chave-teste",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(autenticacao);

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveInterromperAcessoQuandoBancoEstaIndisponivel()
            throws Exception {

        autenticarAdministrador();

        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenThrow(new DataAccessResourceFailureException(
                        "Falha simulada"
                ));

        filtro.doFilter(requisicao, resposta, cadeia);

        assertEquals(503, resposta.getStatus());
        verifyNoInteractions(cadeia);

        
        assertFalse(sessao.isInvalid());

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication());
    }

    private void autenticarAdministrador() {
        var autenticacao =
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin@exemplo.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMINISTRADOR"),
                                FactorGrantedAuthority.fromAuthority(
                                        FactorGrantedAuthority.PASSWORD_AUTHORITY)
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(autenticacao);
    }

    private void simularConta(Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@exemplo.com");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);

        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenReturn(Optional.of(usuario));
    }

    private void verificarEncerramento() {
        assertEquals(302, resposta.getStatus());

        assertEquals(
                "/login?sessao=atualizada",
                resposta.getRedirectedUrl()
        );

        assertTrue(sessao.isInvalid());

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());

        verifyNoInteractions(cadeia);
    }
}