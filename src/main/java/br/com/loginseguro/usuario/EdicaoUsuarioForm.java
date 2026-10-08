package br.com.loginseguro.usuario;

import jakarta.validation.constraints.NotNull;

public class EdicaoUsuarioForm {

    @NotNull(message = "Selecione um perfil.")
    private Perfil perfil;

    @NotNull(message = "Informe a situação da conta.")
    private Boolean ativo;

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}