package br.com.loginseguro.usuario;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EdicaoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;
    private final Validator validator;
    private final FindByIndexNameSessionRepository<? extends Session> sessoes;

    public EdicaoUsuarioService(
            UsuarioRepository usuarioRepository,
            MongoTemplate mongoTemplate,
            Validator validator,
            FindByIndexNameSessionRepository<? extends Session> sessoes) {

        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
        this.validator = validator;
        this.sessoes = sessoes;
    }

    @Transactional
    public void atualizar(String id, EdicaoUsuarioForm formulario) {
        var violacoes = validator.validate(formulario);

        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException(violacoes);
        }

        coordenarEdicao();

        Usuario administrador = buscarAdministradorAtual();

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(UsuarioNaoEncontradoException::new);

        boolean perfilAlterado =
                usuario.getPerfil() != formulario.getPerfil();

        boolean situacaoAlterada =
                usuario.isAtivo() != formulario.getAtivo();

        if (!perfilAlterado && !situacaoAlterada) {
            return;
        }

        if (usuario.getId().equals(administrador.getId())) {
            throw new AlteracaoUsuarioNaoPermitidaException(
                    "Você não pode alterar o próprio perfil "
                    + "ou desativar a sua conta."
            );
        }

        usuario.setPerfil(formulario.getPerfil());
        usuario.setAtivo(formulario.getAtivo());

        usuarioRepository.save(usuario);

        encerrarSessoes(usuario.getEmail());
    }

    private Usuario buscarAdministradorAtual() {
        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null) {
            throw new AccessDeniedException("É necessário estar autenticado.");
        }

        return usuarioRepository.findByEmail(autenticacao.getName())
                .filter(usuario ->
                        usuario.isAtivo()
                        && usuario.getPerfil() == Perfil.ADMINISTRADOR)
                .orElseThrow(() -> new AccessDeniedException(
                        "Sua conta não possui permissão para alterar usuários."
                ));
    }

    private void coordenarEdicao() {
        var resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is("edicao-usuarios")),
                new Update().inc("versao", 1L),
                "controle_administracao"
        );

        if (resultado.getMatchedCount() != 1) {
            throw new IllegalStateException(
                    "O controle de edição de usuários não foi inicializado."
            );
        }
    }

    private void encerrarSessoes(String email) {
        for (String idSessao : sessoes.findByPrincipalName(email).keySet()) {
            sessoes.deleteById(idSessao);
        }
    }
}