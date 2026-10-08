package br.com.loginseguro.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class TemaVisualConfig {

    private final String temaVisual;

    public TemaVisualConfig(
            @Value("${app.tema:claro}") String temaConfigurado) {

        String tema = temaConfigurado.trim()
                .toLowerCase(Locale.ROOT);

        if (!tema.matches("[a-z][a-z0-9-]{0,39}")) {
            throw new IllegalArgumentException(
                    "Nome de tema inválido. Use letras minúsculas, "
                            + "números e hífens, começando por uma letra."
            );
        }

        String caminho = "static/css/temas/" + tema + ".css";

        if (!new ClassPathResource(caminho).exists()) {
            throw new IllegalArgumentException(
                    "Arquivo do tema não encontrado: " + caminho
            );
        }

        this.temaVisual = tema;
    }

    @ModelAttribute("temaVisual")
    public String temaVisual() {
        return temaVisual;
    }
}