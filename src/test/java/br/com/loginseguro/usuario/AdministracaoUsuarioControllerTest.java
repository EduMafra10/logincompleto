package br.com.loginseguro.usuario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import br.com.loginseguro.config.SegurancaConfig;
import br.com.loginseguro.seguranca.VerificacaoSessaoService;

@WebMvcTest(controllers = AdministracaoUsuarioController.class)
@Import({
        SegurancaConfig.class,
        AdministracaoUsuarioService.class
})
@MockitoBean(types = UserDetailsService.class)
class AdministracaoUsuarioControllerTest {

    private static final String HASH_FICTICIO =
            "hash-ficticio-que-nao-deve-aparecer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdministracaoUsuarioService administracaoUsuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private VerificacaoSessaoService verificacaoSessaoService;

    @BeforeEach
    void configurarVerificacaoDasSessoes() {
        when(verificacaoSessaoService.estaValida(any(Authentication.class)))
                .thenReturn(true);
    }

    @Test
    void deveExigirLoginParaListarUsuarios() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(usuarioRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USUARIO", "MODERADOR"})
    void deveNegarListagemParaPerfisSemPermissao(String perfil)
            throws Exception {

        mockMvc.perform(get("/admin/usuarios")
                        .with(user("teste@exemplo.com").roles(perfil)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveListarUsuariosSemExporHashEComEscapeDeHtml()
            throws Exception {

        simularPagina(0, List.of(criarUsuario()), 1);

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Ana &lt;teste&gt;")))
                .andExpect(content().string(
                        containsString("ana@exemplo.com")))
                .andExpect(content().string(
                        containsString("USUARIO")))
                .andExpect(content().string(
                        containsString("Ativo")))
                .andExpect(content().string(
                        containsString(
                                "href=\"/admin/usuarios/usuario-1/editar\"")))
                .andExpect(content().string(
                        not(containsString(HASH_FICTICIO))))
                .andExpect(content().string(
                        not(containsString("Ana <teste>"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveMostrarMensagemQuandoNaoHaUsuarios() throws Exception {
        simularPagina(0, List.of(), 0);

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Nenhum usuário cadastrado.")))
                .andExpect(content().string(
                        not(containsString("Página 1 de 0"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveMostrarPaginacaoNaUltimaPagina() throws Exception {
        simularPagina(1, List.of(criarUsuario()), 21);

        mockMvc.perform(get("/admin/usuarios")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Página 2 de 2")))
                .andExpect(content().string(
                        containsString(
                                "href=\"/admin/usuarios?pagina=0\"")))
                .andExpect(content().string(
                        not(containsString("Próxima"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRedirecionarQuandoPaginaSolicitadaNaoExiste()
            throws Exception {

        simularPagina(9, List.of(), 1);

        mockMvc.perform(get("/admin/usuarios")
                        .param("pagina", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios?pagina=0"));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void deveProtegerServicoContraAcessoDiretoSemPermissao() {
        assertThrows(
                AccessDeniedException.class,
                () -> administracaoUsuarioService.listar(0)
        );

        verifyNoInteractions(usuarioRepository);
    }

    private void simularPagina(
            int numero, List<Usuario> usuarios, long total) {

        PageRequest paginacao = PageRequest.of(
                numero,
                20,
                Sort.by("nome", "email")
        );

        when(usuarioRepository.findAll(paginacao))
                .thenReturn(new PageImpl<>(
                        usuarios,
                        paginacao,
                        total
                ));
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();

        ReflectionTestUtils.setField(usuario, "id", "usuario-1");

        usuario.setNome("Ana <teste>");
        usuario.setEmail("ana@exemplo.com");
        usuario.setSenhaHash(HASH_FICTICIO);
        usuario.setPerfil(Perfil.USUARIO);
        usuario.setAtivo(true);

        return usuario;
    }
}