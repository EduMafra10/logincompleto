package br.com.loginseguro.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdministracaoUsuarioService {

    private static final int USUARIOS_POR_PAGINA = 20;

    private final UsuarioRepository usuarioRepository;

    public AdministracaoUsuarioService(
            UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Page<UsuarioResumo> listar(int pagina) {
        int numeroPagina = Math.max(pagina, 0);

        PageRequest paginacao = PageRequest.of(
                numeroPagina,
                USUARIOS_POR_PAGINA,
                Sort.by("nome", "email")
        );

        return usuarioRepository.findAll(paginacao)
                .map(UsuarioResumo::de);
    }

    public UsuarioResumo buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResumo::de)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }
}