package br.com.loginseguro.usuario;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CadastroUsuarioForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(min = 2, max = 100,
            message = "O nome deve ter entre 2 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
    private String email;

    @NotBlank(message = "Informe uma senha.")
    @Size(min = 15, max = 72,
            message = "A senha deve ter entre 15 e 72 caracteres.")
    private String senha;

    @NotBlank(message = "Confirme sua senha.")
    @Size(max = 72,
            message = "A confirmação deve ter até 72 caracteres.")
    private String confirmacaoSenha;

    @AssertTrue(message = "A senha e a confirmação devem ser iguais.")
    public boolean isSenhasIguais() {
        if (senha == null || confirmacaoSenha == null) {
            return true;
        }

        return senha.equals(confirmacaoSenha);
    }

    @AssertTrue(message = "A senha é muito longa. Use uma senha menor.")
    public boolean isSenhaDentroDoLimite() {
        if (senha == null) {
            return true;
        }

        return senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}