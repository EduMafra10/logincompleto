package br.com.loginseguro.usuario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.loginseguro.config.SegurancaConfig;
import br.com.loginseguro.seguranca.VerificacaoSessaoService;

@WebMvcTest(controllers = EdicaoUsuarioController.class)
@Import(SegurancaConfig.class)
@MockitoBean(types = UserDetailsService.class)
@WithMockUser(
        username = "admin@exemplo.com",
        roles = "ADMINISTRADOR"
)
class EdicaoUsuarioControllerTest {

    private static final String CAMINHO =
            "/admin/usuarios/usuario-1/editar";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministracaoUsuarioService administracaoUsuarioService;

    @MockitoBean
    private EdicaoUsuarioService edicaoUsuarioService;

    @MockitoBean
    private VerificacaoSessaoService verificacaoSessaoService;

    @BeforeEach
    void configurarVerificacaoDasSessoes() {
        when(verificacaoSessaoService.estaValida(any(Authentication.class)))
                .thenReturn(true);
    }

    @Test
    void deveExibirFormularioComDadosDoUsuarioEProtecaoCsrf()
            throws Exception {

        prepararUsuario();

        mockMvc.perform(get(CAMINHO))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Ana Teste")))
                .andExpect(content().string(
                        containsString("ana@exemplo.com")))
                .andExpect(content().string(
                        containsString("name=\"perfil\"")))
                .andExpect(content().string(
                        containsString("name=\"ativo\"")))
                .andExpect(content().string(
                        containsString("name=\"_csrf\"")));

        verifyNoInteractions(edicaoUsuarioService);
    }

    @Test
    void deveSalvarCamposPermitidosUsandoIdDoCaminho()
            throws Exception {

        prepararUsuario();

        mockMvc.perform(post(CAMINHO)
                        .with(csrf())
                        .param("perfil", "MODERADOR")
                        .param("ativo", "false")
                        .param("id", "outro-usuario")
                        .param("email", "alterado@exemplo.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute(
                        "sucesso",
                        "Configuração do usuário salva."
                ));

        ArgumentCaptor<EdicaoUsuarioForm> captor =
                ArgumentCaptor.forClass(EdicaoUsuarioForm.class);

        verify(edicaoUsuarioService).atualizar(
                eq("usuario-1"),
                captor.capture()
        );

        EdicaoUsuarioForm formulario = captor.getValue();

        assertEquals(Perfil.MODERADOR, formulario.getPerfil());
        assertEquals(Boolean.FALSE, formulario.getAtivo());
    }

    @Test
    void deveRecusarEnvioSemTokenCsrf() throws Exception {
        mockMvc.perform(post(CAMINHO)
                        .param("perfil", "MODERADOR")
                        .param("ativo", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                administracaoUsuarioService,
                edicaoUsuarioService
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"USUARIO", "MODERADOR"})
    void deveRecusarEdicaoPorPerfilSemPermissao(String perfil)
            throws Exception {

        mockMvc.perform(post(CAMINHO)
                        .with(user("teste@exemplo.com").roles(perfil))
                        .with(csrf())
                        .param("perfil", "ADMINISTRADOR")
                        .param("ativo", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                administracaoUsuarioService,
                edicaoUsuarioService
        );
    }

    @ParameterizedTest
    @CsvSource({
            "perfil, INVALIDO, true",
            "ativo, USUARIO, INVALIDO",
            "perfil, '', true",
            "ativo, USUARIO, ''"
    })
    void deveRecusarCamposInvalidosOuAusentes(
            String campo,
            String perfil,
            String ativo) throws Exception {

        prepararUsuario();

        mockMvc.perform(post(CAMINHO)
                        .with(csrf())
                        .param("perfil", perfil)
                        .param("ativo", ativo))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors(
                        "edicao", campo));

        verifyNoInteractions(edicaoUsuarioService);
    }

    @Test
    void deveExibirMensagemQuandoAlteracaoNaoEPermitida()
            throws Exception {

        prepararUsuario();

        String mensagem =
                "Você não pode alterar o próprio perfil "
                + "ou desativar a sua conta.";

        doThrow(new AlteracaoUsuarioNaoPermitidaException(mensagem))
                .when(edicaoUsuarioService)
                .atualizar(
                        eq("usuario-1"),
                        any(EdicaoUsuarioForm.class)
                );

        mockMvc.perform(post(CAMINHO)
                        .with(csrf())
                        .param("perfil", "MODERADOR")
                        .param("ativo", "true"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString(mensagem)));
    }

    @Test
    void deveTratarFalhaTemporariaSemExporDetalhesDoBanco()
            throws Exception {

        prepararUsuario();

        doThrow(new TransientDataAccessResourceException(
                "detalhe-interno-do-banco"))
                .when(edicaoUsuarioService)
                .atualizar(
                        eq("usuario-1"),
                        any(EdicaoUsuarioForm.class)
                );

        mockMvc.perform(post(CAMINHO)
                        .with(csrf())
                        .param("perfil", "MODERADOR")
                        .param("ativo", "true"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(
                        containsString(
                                "Não foi possível confirmar a alteração.")))
                .andExpect(content().string(
                        not(containsString("detalhe-interno-do-banco"))));
    }

    @Test
    void deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
        when(administracaoUsuarioService.buscarPorId("inexistente"))
                .thenThrow(new UsuarioNaoEncontradoException());

        mockMvc.perform(get(
                        "/admin/usuarios/{id}/editar",
                        "inexistente"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(edicaoUsuarioService);
    }

    private void prepararUsuario() {
        UsuarioResumo usuario = new UsuarioResumo(
                "usuario-1",
                "Ana Teste",
                "ana@exemplo.com",
                Perfil.USUARIO,
                true
        );

        when(administracaoUsuarioService.buscarPorId("usuario-1"))
                .thenReturn(usuario);
    }
}