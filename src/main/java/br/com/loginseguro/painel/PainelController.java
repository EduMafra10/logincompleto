package br.com.loginseguro.painel;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PainelController {

    @GetMapping("/admin")
    public String exibirAdministracao(Model model) {
        model.addAttribute("titulo", "Administração");
        model.addAttribute("descricao", "Área de administração do sistema.");

        return "painel";
    }

    @GetMapping("/moderacao")
    public String exibirModeracao(Model model) {
        model.addAttribute("titulo", "Moderação");
        model.addAttribute("descricao", "Área de acompanhamento e moderação.");

        return "painel";
    }
}