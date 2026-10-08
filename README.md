# Login Seguro

Sistema de autenticação e controle de acesso desenvolvido para a disciplina Aplicativos WEB, do curso de Sistemas de Informação da UMC.

**Autor:** Eduardo Mafra dos Santos.

## Funcionalidades

- Cadastro com validação dos dados e verificação de e-mail duplicado.
- Armazenamento de senhas com hash BCrypt.
- Login e logout com Spring Security.
- Persistência dos usuários e das sessões no MongoDB Atlas.
- Três perfis de acesso: USUARIO, MODERADOR e ADMINISTRADOR.
- Proteção das rotas e apresentação de página de acesso negado.
- Listagem paginada de usuários para administradores.
- Alteração do perfil e ativação ou desativação das contas.
- Proteção contra alteração do próprio perfil ou desativação da própria conta pelo administrador.
- Encerramento de sessões quando as permissões ficam desatualizadas ou a conta é desativada.
- Temas claro e escuro selecionáveis por configuração.
- Testes automatizados das regras de negócio, dos controladores e do controle de acesso.

## Tecnologias utilizadas

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data MongoDB
- Spring Session com MongoDB
- Thymeleaf
- Bean Validation
- JUnit e Mockito
- Maven Wrapper

## Requisitos do ambiente

- JDK 21 instalado e configurado.
- Git instalado.
- Acesso à internet para obter as dependências e conectar ao Atlas.
- Cluster MongoDB Atlas disponível.
- Usuário do banco com permissão de leitura e escrita no banco `loginseguro`.
- IP da máquina de execução autorizado na lista de acesso do Atlas.

O Maven Wrapper está incluído no projeto.

## Obter o código

```powershell
git clone https://github.com/EduMafra10/logincompleto.git
cd logincompleto
```

Execute os comandos seguintes na pasta que contém `pom.xml` e `mvnw.cmd`.

## Configurar a conexão com o MongoDB Atlas

1. No Atlas, configure o cluster e autorize o IP da máquina.
2. Crie um usuário de banco com acesso ao banco `loginseguro`.
3. Obtenha a URI de conexão fornecida pelo Atlas.
4. Se ainda não existir, crie um arquivo `.env` na raiz do projeto, usando `.env.example` como modelo.
5. Preencha `MONGODB_URI` com os dados reais da conexão.

Exemplo com valores fictícios:

```properties
MONGODB_URI=mongodb+srv://USUARIO_DO_BANCO:SENHA_CODIFICADA@HOST_DO_CLUSTER/?appName=logincompleto
```

Substitua o usuário, a senha e o host pelos valores do seu ambiente. Caracteres reservados na senha precisam ser codificados para utilização em uma URI.

O usuário de conexão do Atlas é diferente das contas cadastradas pela interface da aplicação.

O arquivo `.env` contém credenciais locais e não deve ser enviado ao GitHub. O `.env.example` contém somente valores de exemplo.

A configuração da aplicação inclui:

```properties
spring.config.import=optional:file:.env[.properties]
spring.mongodb.uri=${MONGODB_URI}
spring.mongodb.database=loginseguro
spring.data.mongodb.auto-index-creation=true
app.tema=escuro
```

Essas propriedades ficam em `src/main/resources/application.properties`.

A URI é carregada do ambiente, evitando colocar a senha diretamente no código-fonte.

## Executar localmente

No PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse:

- [Login](http://localhost:8080/login)
- [Cadastro](http://localhost:8080/cadastro)

Para encerrar a aplicação, pressione `Ctrl + C` no terminal em que ela está rodando.

Se a porta 8080 estiver ocupada por outra execução do projeto, encerre essa execução antes de iniciar novamente.

## Configurar o primeiro administrador

As contas criadas pela tela de cadastro recebem o perfil `USUARIO`.

Para preparar o administrador inicial do ambiente:

1. Cadastre sua conta pela aplicação.
2. No Data Explorer do Atlas, abra o banco `loginseguro`.
3. Acesse a coleção `usuarios`.
4. Localize sua conta pelo e-mail.
5. Altere o campo `perfil` para `ADMINISTRADOR`, mantendo `ativo` como `true`.
6. Salve e faça um novo login.

Depois dessa configuração inicial, utilize a interface administrativa para gerenciar o perfil e a situação das demais contas.

## Controle de acesso

| Rota | USUARIO | MODERADOR | ADMINISTRADOR |
|---|---|---|---|
| `/inicio` | Permitido | Permitido | Permitido |
| `/moderacao` | Negado | Permitido | Permitido |
| `/admin` | Negado | Negado | Permitido |
| `/admin/usuarios` | Negado | Negado | Permitido |

Visitantes sem autenticação são encaminhados ao login ao tentar acessar páginas protegidas.

Contas inativas não podem autenticar. Quando o perfil de uma conta muda ou ela é desativada, sua sessão anterior deixa de permitir o acesso.

## Integração com o MongoDB

A aplicação utiliza o banco `loginseguro`.

As principais coleções são:

- `usuarios`: dados cadastrais, hash da senha, perfil e situação da conta.
- `sessions`: sessões persistidas pelo Spring Session.
- `controle_administracao`: documento utilizado para coordenar alterações administrativas dentro das transações.

O Spring Data MongoDB realiza o acesso aos dados. As regras de negócio ficam nos serviços, enquanto os controladores recebem as requisições e preparam os dados das páginas.

## Temas visuais

O tema padrão configurado no projeto é o escuro.

Em `src/main/resources/application.properties`:

```properties
app.tema=escuro
```

Para utilizar o tema claro:

```properties
app.tema=claro
```

Reinicie a aplicação após alterar essa configuração. Se necessário, atualize o navegador com `Ctrl + F5`.

### Organização visual

- `static/css/estilo.css`: estrutura dos componentes, espaçamento, formulários e tabelas.
- `static/css/temas/claro.css`: cores do tema claro.
- `static/css/temas/escuro.css`: cores do tema escuro.
- `templates/fragmentos/layout.html`: cabeçalho compartilhado, título e carregamento dos estilos.
- `TemaVisualConfig`: disponibiliza o tema configurado para os templates.

Os caminhos de CSS e templates são relativos a `src/main/resources`.

### Adicionar outro tema

1. Copie um dos arquivos de tema para a mesma pasta.
2. Renomeie a cópia, por exemplo, para `personalizado.css`.
3. Ajuste as cores, preservando os nomes das variáveis CSS.
4. Configure:

```properties
app.tema=personalizado
```

5. Reinicie a aplicação.

Use nomes de tema com até 40 caracteres, começando por uma letra e contendo letras minúsculas, números ou hífens.

Essa organização permite modificar a identidade visual sem alterar as regras de autenticação e de negócio.

## Organização do código

Dentro de `src/main/java/br/com/loginseguro`:

- `config`: configurações de segurança, senhas, sessões, transações e tema.
- `seguranca`: autenticação e verificação das sessões.
- `usuario`: cadastro, consulta e gerenciamento de usuários.
- `painel`: páginas associadas aos perfis de acesso.

Outros diretórios:

- `src/main/resources/templates`: páginas Thymeleaf.
- `src/main/resources/static`: recursos estáticos.
- `src/test/java`: testes automatizados.

## Executar os testes e gerar o pacote

```powershell
.\mvnw.cmd clean verify
```

Esse comando recompila o projeto, executa os testes configurados e gera o pacote executável na pasta `target`.

Os relatórios dos testes ficam em `target/surefire-reports`.

O teste de carregamento do contexto utiliza a configuração da aplicação. Mantenha o `.env` configurado e o MongoDB acessível ao executar a suíte completa.

## Fluxo de desenvolvimento

O projeto utiliza a organização do Gitflow:

- `main`: versão de entrega.
- `develop`: integração das funcionalidades.
- `feature/*`: desenvolvimento das funcionalidades.
- `release/*`: preparação da versão de entrega.

As alterações são validadas antes de sua integração na versão final.