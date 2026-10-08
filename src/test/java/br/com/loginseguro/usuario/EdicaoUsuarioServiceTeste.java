package br.com.loginseguro.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mongodb.client.result.UpdateResult;

import jakarta.validation.ConstraintViolationException;

@SpringJUnitConfig(EdicaoUsuarioServiceTest.Config.class)
@WithMockUser(
        username = "admin@exemplo.com",
        roles = "ADMINISTRADOR"
)
class EdicaoUsuarioServiceTest {

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(EdicaoUsuarioService.class)
    static class Config {

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Autowired
    private EdicaoUsuarioService service;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private FindByIndexNameSessionRepository<MapSession> sessoes;

    @ParameterizedTest
    @CsvSource({
            "MODERADOR, true",
            "USUARIO, false"
    })
    void deveAlterarContaEEncerrarSuasSessoes(
            Perfil perfil, boolean ativo) {

        prepararAdministrador();

        Usuario usuario = criarUsuario(
                "usuario-1", "ana@exemplo.com", Perfil.USUARIO, true);

        when(usuarioRepository.findById("usuario-1"))
                .thenReturn(Optional.of(usuario));

        MapSession primeiraSessao = new MapSession();
        MapSession segundaSessao = new MapSession();

        when(sessoes.findByPrincipalName("ana@exemplo.com"))
                .thenReturn(Map.of(
                        primeiraSessao.getId(), primeiraSessao,
                        segundaSessao.getId(), segundaSessao
                ));

        service.atualizar(
                "usuario-1", formulario(perfil, ativo));

        assertEquals(perfil, usuario.getPerfil());
        assertEquals(ativo, usuario.isAtivo());

        
        assertEquals("Pessoa de teste", usuario.getNome());
        assertEquals("ana@exemplo.com", usuario.getEmail());
        assertEquals("hash-ficticio", usuario.getSenhaHash());

        verify(usuarioRepository).save(usuario);

        verify(sessoes).findByPrincipalName("ana@exemplo.com");
        verify(sessoes).deleteById(primeiraSessao.getId());
        verify(sessoes).deleteById(segundaSessao.getId());
        verifyNoMoreInteractions(sessoes);
    }

    @ParameterizedTest
    @CsvSource({
            "MODERADOR, true",
            "ADMINISTRADOR, false"
    })
    void deveImpedirAlteracaoDoProprioPerfilOuDesativacao(
            Perfil perfil, boolean ativo) {

        Usuario administrador = prepararAdministrador();

        when(usuarioRepository.findById("admin-1"))
                .thenReturn(Optional.of(administrador));

        assertThrows(
                AlteracaoUsuarioNaoPermitidaException.class,
                () -> service.atualizar(
                        "admin-1", formulario(perfil, ativo))
        );

        assertEquals(
                Perfil.ADMINISTRADOR, administrador.getPerfil());
        assertEquals(true, administrador.isAtivo());

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(sessoes);
    }

    @Test
    void devePreservarSessoesQuandoNaoHaAlteracao() {
        Usuario administrador = prepararAdministrador();

        when(usuarioRepository.findById("admin-1"))
                .thenReturn(Optional.of(administrador));

        service.atualizar(
                "admin-1",
                formulario(Perfil.ADMINISTRADOR, true)
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(sessoes);
    }

    @ParameterizedTest
    @CsvSource({
            "MODERADOR, true",
            "ADMINISTRADOR, false"
    })
    void deveRecusarAdministradorQuePerdeuPermissaoNoBanco(
            Perfil perfilAtual, boolean ativoAtual) {

        prepararControle();

        Usuario administrador = criarUsuario(
                "admin-1",
                "admin@exemplo.com",
                perfilAtual,
                ativoAtual
        );

        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenReturn(Optional.of(administrador));

        
        assertThrows(
                AccessDeniedException.class,
                () -> service.atualizar(
                        "usuario-1",
                        formulario(Perfil.MODERADOR, true))
        );

        verify(usuarioRepository, never()).findById(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(sessoes);
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "null, true",
                    "USUARIO, null"
            },
            nullValues = "null"
    )
    void deveRecusarFormularioIncompleto(
            Perfil perfil, Boolean ativo) {

        assertThrows(
                ConstraintViolationException.class,
                () -> service.atualizar(
                        "usuario-1", formulario(perfil, ativo))
        );

        verifyNoInteractions(
                usuarioRepository, mongoTemplate, sessoes);
    }

    @Test
    void deveInformarQuandoUsuarioNaoExiste() {
        prepararAdministrador();

        when(usuarioRepository.findById("inexistente"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> service.atualizar(
                        "inexistente",
                        formulario(Perfil.MODERADOR, true))
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(sessoes);
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void deveImpedirEdicaoPorUsuarioComum() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.atualizar(
                        "usuario-1",
                        formulario(Perfil.ADMINISTRADOR, true))
        );

        verifyNoInteractions(
                usuarioRepository, mongoTemplate, sessoes);
    }

    @Test
    @WithMockUser(roles = "MODERADOR")
    void deveImpedirEdicaoPorModerador() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.atualizar(
                        "usuario-1",
                        formulario(Perfil.ADMINISTRADOR, true))
        );

        verifyNoInteractions(
                usuarioRepository, mongoTemplate, sessoes);
    }

    private void prepararControle() {
        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("controle_administracao")
        )).thenReturn(UpdateResult.acknowledged(1L, 1L, null));
    }

    private Usuario prepararAdministrador() {
        prepararControle();

        Usuario administrador = criarUsuario(
                "admin-1",
                "admin@exemplo.com",
                Perfil.ADMINISTRADOR,
                true
        );

        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenReturn(Optional.of(administrador));

        return administrador;
    }

    private Usuario criarUsuario(
            String id, String email, Perfil perfil, boolean ativo) {

        Usuario usuario = new Usuario();

        ReflectionTestUtils.setField(usuario, "id", id);

        usuario.setNome("Pessoa de teste");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-ficticio");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);

        return usuario;
    }

    private EdicaoUsuarioForm formulario(
            Perfil perfil, Boolean ativo) {

        EdicaoUsuarioForm formulario = new EdicaoUsuarioForm();
        formulario.setPerfil(perfil);
        formulario.setAtivo(ativo);

        return formulario;
    }
}