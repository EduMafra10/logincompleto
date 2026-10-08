package br.com.loginseguro.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/usuarios")
public class EdicaoUsuarioController {

    private static final Logger log =
            LoggerFactory.getLogger(EdicaoUsuarioController.class);

    private final AdministracaoUsuarioService administracaoUsuarioService;
    private final EdicaoUsuarioService edicaoUsuarioService;

    public EdicaoUsuarioController(
            AdministracaoUsuarioService administracaoUsuarioService,
            EdicaoUsuarioService edicaoUsuarioService) {

        this.administracaoUsuarioService = administracaoUsuarioService;
        this.edicaoUsuarioService = edicaoUsuarioService;
    }

    @InitBinder("edicao")
    public void configurarCamposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("perfil", "ativo");
    }

    @GetMapping("/{id}/editar")
    public String exibirFormulario(
            @PathVariable("id") String id,
            Model model) {

        UsuarioResumo usuario =
                administracaoUsuarioService.buscarPorId(id);

        EdicaoUsuarioForm formulario = new EdicaoUsuarioForm();
        formulario.setPerfil(usuario.perfil());
        formulario.setAtivo(usuario.ativo());

        model.addAttribute("edicao", formulario);
        preencherModelo(usuario, model);

        return "admin/editar-usuario";
    }

    @PostMapping("/{id}/editar")
    public String salvar(
            @PathVariable("id") String id,
            @Valid @ModelAttribute("edicao") EdicaoUsuarioForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirecionamento,
            HttpServletResponse resposta) {

        UsuarioResumo usuario =
                administracaoUsuarioService.buscarPorId(id);

        preencherModelo(usuario, model);

        if (resultado.hasErrors()) {
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "admin/editar-usuario";
        }

        try {
            edicaoUsuarioService.atualizar(id, formulario);

            redirecionamento.addFlashAttribute(
                    "sucesso",
                    "Configuração do usuário salva."
            );

            return "redirect:/admin/usuarios";

        } catch (AlteracaoUsuarioNaoPermitidaException ex) {
            resultado.reject(
                    "alteracaoNaoPermitida",
                    ex.getMessage()
            );

            resposta.setStatus(HttpServletResponse.SC_CONFLICT);

        } catch (TransientDataAccessException | TransactionException ex) {
            log.warn("Falha ao confirmar a edição do usuário.", ex);

            resultado.reject(
                    "falhaAoConfirmar",
                    "Não foi possível confirmar a alteração. "
                    + "Volte à lista, confira a situação atual "
                    + "e tente novamente se necessário."
            );

            resposta.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        return "admin/editar-usuario";
    }

    private void preencherModelo(UsuarioResumo usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", Perfil.values());
    }
}