package br.com.loginseguro.usuario;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@Controller
public class CadastroController {

    private final UsuarioService usuarioService;

    public CadastroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @InitBinder("cadastro")
    public void configurarCampos(WebDataBinder binder) {
        binder.setAllowedFields(
                "nome",
                "email",
                "senha",
                "confirmacaoSenha");
    }

    @GetMapping("/cadastro")
    public String exibirCadastro(Model model) {
        model.addAttribute("cadastro", new CadastroUsuarioForm());
        return "autenticacao/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @ModelAttribute("cadastro") CadastroUsuarioForm formulario,
            BindingResult resultado) {

        if (resultado.hasErrors()) {
            limparSenhas(formulario);
            return "autenticacao/cadastro";
        }

        try {
            usuarioService.cadastrar(formulario);
            return "redirect:/login?cadastro";

        } catch (ConstraintViolationException exception) {
            for (ConstraintViolation<?> erro
                    : exception.getConstraintViolations()) {

                resultado.reject("cadastro.invalido", erro.getMessage());
            }

        } catch (EmailJaCadastradoException exception) {
            resultado.reject(
                    "cadastro.emailDuplicado",
                    exception.getMessage());
        }

        limparSenhas(formulario);
        return "autenticacao/cadastro";
    }

    private void limparSenhas(CadastroUsuarioForm formulario) {
        formulario.setSenha(null);
        formulario.setConfirmacaoSenha(null);
    }
}