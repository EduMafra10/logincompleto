package br.com.loginseguro.seguranca;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class AutenticacaoController {
    
    @GetMapping("/")
    public String abrirPaginaInicial() {
        return "redirect:/login";
    }

    @GetMapping ("/login")
    public String exibirLogin() {
        return "autenticacao/login";
    }
}