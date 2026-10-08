package br.com.loginseguro.seguranca;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import br.com.loginseguro.usuario.Usuario;
import br.com.loginseguro.usuario.UsuarioRepository;

@Service
public class VerificacaoSessaoService {

    private final UsuarioRepository usuarioRepository;

    public VerificacaoSessaoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean estaValida(Authentication autenticacao) {
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return false;
        }

        Set<String> perfisDaSessao = autenticacao.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(permissao -> permissao.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return usuarioRepository.findByEmail(autenticacao.getName())
                .filter(Usuario::isAtivo)
                .filter(usuario -> usuario.getPerfil() != null)
                .map(usuario -> {
                    String perfilAtual =
                            "ROLE_" + usuario.getPerfil().name();

                    return perfisDaSessao.equals(Set.of(perfilAtual));
                })
                .orElse(false);
    }
}