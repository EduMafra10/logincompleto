package br.com.loginseguro.seguranca;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AutenticacaoController {

    @GetMapping("/")
    public String abrirPaginaInicial() {
        return "redirect:/inicio";
    }

    @GetMapping("/login")
    public String exibirLogin() {
        return "autenticacao/login";
    }

    @GetMapping("/inicio")
    public String exibirInicio(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "inicio";
    }
}