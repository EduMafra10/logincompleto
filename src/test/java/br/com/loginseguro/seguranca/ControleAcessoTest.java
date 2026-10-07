package br.com.loginseguro.seguranca;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.loginseguro.config.SegurancaConfig;
import br.com.loginseguro.painel.PainelController;

@WebMvcTest(controllers = {
        AutenticacaoController.class,
        PainelController.class
})
@Import(SegurancaConfig.class)
@MockitoBean(types = UserDetailsService.class)
class ControleAcessoTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/inicio", "/admin", "/moderacao"})
    void deveExigirLoginParaAcessarPaginasProtegidas(String caminho)
            throws Exception {

        mockMvc.perform(get(caminho))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest
    @CsvSource({
            "USUARIO, /admin, 403",
            "USUARIO, /moderacao, 403",
            "MODERADOR, /admin, 403",
            "MODERADOR, /moderacao, 200",
            "ADMINISTRADOR, /admin, 200",
            "ADMINISTRADOR, /moderacao, 200"
    })
    void deveControlarAcessoConformePerfil(
            String perfil,
            String caminho,
            int statusEsperado) throws Exception {

        mockMvc.perform(get(caminho)
                        .with(user("teste@exemplo.com").roles(perfil)))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    @WithMockUser(username = "usuario@exemplo.com", roles = "USUARIO")
    void deveOcultarLinksRestritosParaUsuarioComum() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "email", "usuario@exemplo.com"))
                .andExpect(content().string(
                        not(containsString("href=\"/admin\""))))
                .andExpect(content().string(
                        not(containsString("href=\"/moderacao\""))));
    }

    @Test
    @WithMockUser(username = "moderador@exemplo.com", roles = "MODERADOR")
    void deveMostrarSomenteLinkDeModeracaoParaModerador() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("href=\"/moderacao\"")))
                .andExpect(content().string(
                        not(containsString("href=\"/admin\""))));
    }

    @Test
    @WithMockUser(
            username = "administrador@exemplo.com",
            roles = "ADMINISTRADOR")
    void deveMostrarOsDoisLinksParaAdministrador() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("href=\"/admin\"")))
                .andExpect(content().string(
                        containsString("href=\"/moderacao\"")));
    }
}