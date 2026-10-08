package br.com.loginseguro.usuario;

public record UsuarioResumo(
        String id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo) {

    public static UsuarioResumo de(Usuario usuario) {
        return new UsuarioResumo(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo()
        );
    }
}