package br.com.loginseguro.usuario;

public class EmailJaCadastradoException extends RuntimeException {
    
    public EmailJaCadastradoException() {
        super("Já existe uma conta com este e-mail.");
    }

    public EmailJaCadastradoException(Throwable causa) {
        super("Já existe uma conta com este e-mail.", causa);
    }
}
