package br.com.loginseguro.usuario;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/usuarios")
public class AdministracaoUsuarioController {

    private final AdministracaoUsuarioService administracaoUsuarioService;

    public AdministracaoUsuarioController(
            AdministracaoUsuarioService administracaoUsuarioService) {
        this.administracaoUsuarioService = administracaoUsuarioService;
    }

    @GetMapping
    public String listar(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            Model model) {

        Page<UsuarioResumo> paginaUsuarios =
                administracaoUsuarioService.listar(pagina);

        if (paginaUsuarios.isEmpty() && paginaUsuarios.getNumber() > 0) {
            int ultimaPagina = Math.max(
                    paginaUsuarios.getTotalPages() - 1, 0);

            return "redirect:/admin/usuarios?pagina=" + ultimaPagina;
        }

        model.addAttribute("paginaUsuarios", paginaUsuarios);

        return "admin/usuarios";
    }
}