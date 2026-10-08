package br.com.loginseguro.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlteracaoUsuarioNaoPermitidaException extends RuntimeException {

    public AlteracaoUsuarioNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}