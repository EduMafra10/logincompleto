package br.com.loginseguro.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.loginseguro.config.SenhaConfig;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private ValidatorFactory validatorFactory;
    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new SenhaConfig().passwordEncoder();
        validatorFactory = Validation.buildDefaultValidatorFactory();

        usuarioService = new UsuarioService(
                usuarioRepository,
                passwordEncoder,
                validatorFactory.getValidator());
    }

    @AfterEach
    void finalizar() {
        validatorFactory.close();
    }

    @Test
    void deveCadastrarUsuarioComDadosNormalizadosESenhaProtegida() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        formulario.setNome("  Ana Silva  ");
        formulario.setEmail("  ANA@EXEMPLO.COM  ");

        usuarioService.cadastrar(formulario);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuario = captor.getValue();

        assertEquals("Ana Silva", usuario.getNome());
        assertEquals("ana@exemplo.com", usuario.getEmail());
        assertEquals(Perfil.USUARIO, usuario.getPerfil());
        assertTrue(usuario.isAtivo());

        assertNotEquals(formulario.getSenha(), usuario.getSenhaHash());
        assertTrue(passwordEncoder.matches(
                formulario.getSenha(),
                usuario.getSenhaHash()));
    }

    @Test
    void deveRecusarNomeCurtoAposRemoverEspacos() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        formulario.setNome(" A ");

        verificarCadastroInvalido(formulario);
    }

    @Test
    void deveRecusarEmailInvalido() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        formulario.setEmail("email-invalido");

        verificarCadastroInvalido(formulario);
    }

    @Test
    void deveRecusarSenhaCurta() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        formulario.setSenha("12345678");
        formulario.setConfirmacaoSenha("12345678");

        verificarCadastroInvalido(formulario);
    }

    @Test
    void deveRecusarSenhasDiferentes() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        formulario.setConfirmacaoSenha("Outra senha de teste 456");

        verificarCadastroInvalido(formulario);
    }

    @Test
    void deveRecusarSenhaComMaisDe72Bytes() {
        CadastroUsuarioForm formulario = criarFormularioValido();
        String senha = "á".repeat(37);

        formulario.setSenha(senha);
        formulario.setConfirmacaoSenha(senha);

        verificarCadastroInvalido(formulario);
    }

    @Test
    void deveRecusarEmailJaCadastrado() {
        CadastroUsuarioForm formulario = criarFormularioValido();

        when(usuarioRepository.existsByEmail("ana@exemplo.com"))
                .thenReturn(true);

        assertThrows(
                EmailJaCadastradoException.class,
                () -> usuarioService.cadastrar(formulario));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveTratarDuplicidadeIdentificadaAoSalvar() {
        CadastroUsuarioForm formulario = criarFormularioValido();

        when(usuarioRepository.existsByEmail("ana@exemplo.com"))
                .thenReturn(false);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new DuplicateKeyException("E-mail duplicado"));

        assertThrows(
                EmailJaCadastradoException.class,
                () -> usuarioService.cadastrar(formulario));

        verify(usuarioRepository).save(any(Usuario.class));
    }

    private void verificarCadastroInvalido(
            CadastroUsuarioForm formulario) {

        assertThrows(
                ConstraintViolationException.class,
                () -> usuarioService.cadastrar(formulario));

        verifyNoInteractions(usuarioRepository);
    }

    private CadastroUsuarioForm criarFormularioValido() {
        CadastroUsuarioForm formulario = new CadastroUsuarioForm();

        formulario.setNome("Ana Silva");
        formulario.setEmail("ana@exemplo.com");
        formulario.setSenha("Uma senha de teste 123");
        formulario.setConfirmacaoSenha("Uma senha de teste 123");

        return formulario;
    }
}