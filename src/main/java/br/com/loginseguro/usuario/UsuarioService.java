package br.com.loginseguro.usuario;

import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            Validator validator) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    public void cadastrar(CadastroUsuarioForm formulario) {
        normalizarDados(formulario);
        validarDados(formulario);

        if (usuarioRepository.existsByEmail(formulario.getEmail())) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNome(formulario.getNome());
        usuario.setEmail(formulario.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(formulario.getSenha()));
        usuario.setPerfil(Perfil.USUARIO);
        usuario.setAtivo(true);

        try {
            usuarioRepository.save(usuario);
        } catch (DuplicateKeyException exception) {
            throw new EmailJaCadastradoException(exception);
        }
    }

    private void normalizarDados(CadastroUsuarioForm formulario) {
        if (formulario.getNome() != null) {
            formulario.setNome(formulario.getNome().strip());
        }

        if (formulario.getEmail() != null) {
            String email = formulario.getEmail()
                    .strip()
                    .toLowerCase(Locale.ROOT);

            formulario.setEmail(email);
        }
    }

    private void validarDados(CadastroUsuarioForm formulario) {
        Set<ConstraintViolation<CadastroUsuarioForm>> erros =
                validator.validate(formulario);

        if (!erros.isEmpty()) {
            throw new ConstraintViolationException(erros);
        }
    }
}