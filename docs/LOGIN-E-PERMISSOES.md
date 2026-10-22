# Login e permissões — guia para estudar o ResolveIT

O sistema tem dois perfis de acesso. O cargo profissional (`TECNICO` ou `SOLICITANTE`) continua existindo, mas quem decide as permissões é o novo campo `perfil`.

| Ação | USUARIO | ADMIN |
| --- | --- | --- |
| Criar chamado | Em seu próprio nome, aberto | Para qualquer pessoa, com qualquer status |
| Listar e consultar chamados | Somente os próprios | Todos |
| Editar título, descrição ou status | Não | Sim |
| Excluir chamado | Não | Sim |
| Listar, cadastrar e editar pessoas | Não | Sim |
| Conceder perfil de administrador | Não | Sim |

Admins podem concluir ou fechar diretamente, sem passar por todas as etapas, e reabrir chamados. Um admin não pode excluir a própria conta nem retirar o próprio perfil, evitando perder o último acesso administrativo. A regra que impede excluir pessoas com chamados continua valendo.

## 1. Rodar e criar seu administrador

1. No IntelliJ, sincronize o Maven pelo `backend/pom.xml`. Ele baixará o Spring Security.
2. Abra **Run → Edit Configurations → ResolveitApplication → Environment variables**. Se o campo estiver oculto, procure **Modify options → Environment variables**.
3. Mantenha `POSTGRE_PASSWORD` com a senha do PostgreSQL. Adicione estas duas variáveis separadas, com valores escolhidos por você:

   | Variável | Valor |
   | --- | --- |
   | `RESOLVEIT_ADMIN_EMAIL` | Um e-mail que ainda não esteja cadastrado |
   | `RESOLVEIT_ADMIN_PASSWORD` | Sua senha de 8 a 64 caracteres |

4. Pare e execute novamente o backend. `AdminInicialConfig` cria a conta com nome “Administrador”. Não há senha padrão no código.
5. No frontend, execute `npm.cmd run dev` e abra `http://localhost:5173`. Entre com o e-mail e a senha escolhidos.
6. Depois de confirmar o acesso, remova as duas variáveis de criação inicial da configuração da IDE. A conta já está salva no banco e continuará funcionando.
7. Em **Pessoas → Nova pessoa**, escolha **Usuário** ou **Administrador**. Também é possível mudar o perfil de uma pessoa existente.

Se o e-mail inicial já pertencer a um usuário comum, a aplicação avisa e não o promove automaticamente. Use um e-mail novo. Se já pertencer a um admin, reiniciar não substitui sua senha.

Usuários comuns podem escolher **Criar conta** na tela de login. Esse cadastro sempre cria `USUARIO`, mesmo que alguém envie `"perfil":"ADMIN"` manualmente à API.

## 2. O caminho de um cadastro no Java

1. **Tela (`AuthScreen.tsx`):** coleta nome, e-mail, senha e setor.
2. **Cliente HTTP (`api.ts`):** busca um token CSRF e envia os dados para `POST /api/auth/cadastro`. O proxy do Vite retira `/api` antes de chamar o Java.
3. **DTO (`CadastroUsuarioRequest`):** define os campos aceitos. `@NotBlank`, `@Email`, `@Size` e `@NotNull` validam os valores.
4. **Controller (`AuthController`):** recebe o JSON e cria uma entidade `Usuario` com perfil comum. O DTO público não aceita definir permissões.
5. **Service (`UsuarioService`):** normaliza o e-mail, verifica duplicidade e transforma a senha em um hash BCrypt.
6. **Repository (`UsuarioRepository`):** o Spring Data JPA grava a entidade na tabela `usuarios` do PostgreSQL.
7. **Resposta:** o servidor devolve HTTP 201. A tela informa que a conta foi criada e pede o login.

Um DTO é o formato dos dados que entram ou saem da API. A entidade representa os dados do banco. O service aplica as regras; o repository faz o acesso ao banco.

## 3. O caminho do login

1. A tela envia e-mail e senha para `POST /auth/login`, como formulário HTTP.
2. O filtro do **Spring Security**, configurado em `SecurityConfig`, recebe esse login. Não precisamos escrever um controller manual para comparar senhas.
3. `UserDetailsService` busca a pessoa pelo e-mail. `DaoAuthenticationProvider` usa o `PasswordEncoder` para comparar a senha informada com o hash salvo.
4. Se estiver correta, o Spring troca o identificador da sessão e envia o cookie `JSESSIONID`. A senha não fica no `localStorage` nem vai nas respostas de consulta.
5. O navegador envia o cookie nas próximas chamadas ao mesmo endereço. `GET /auth/me` devolve nome, ID, setor e perfil para montar a tela.
6. **Sair** chama `POST /auth/logout`, que invalida a sessão. Após 30 minutos sem atividade no servidor, a sessão expira. Reiniciar o backend também encerra as sessões em memória.

BCrypt não é uma senha criptografada que possa ser descriptografada: é um hash usado para conferir se a senha apresentada está correta. Senhas têm limite adicional de 72 bytes, por causa do BCrypt; caracteres acentuados podem ocupar mais de um byte.

## 4. Como impedimos acesso aos chamados dos outros

`UsuarioAutenticado.atual()` lê o ID da sessão e consulta a pessoa no banco. O ID permanece válido mesmo se o admin editar o e-mail.

Em `ChamadoService.listar`, quando o perfil é `USUARIO`, o servidor força `solicitanteId` a ser o ID autenticado. Enviar o ID de outra pessoa na URL não muda o resultado. Isso também protege os totais e a paginação do painel.

Em `buscarPorId`, o servidor compara o dono do chamado com a pessoa autenticada. Um chamado de outra pessoa retorna 404, sem revelar se ele existe.

Ao criar um chamado, o servidor usa a pessoa autenticada. Um usuário que tente escolher outra pessoa ou definir um status diferente de aberto recebe 403.

Os métodos de edição e exclusão usam `@PreAuthorize("@usuarioAutenticado.isAdmin()")`. A área de pessoas também exige admin. Essa verificação consulta o perfil atual no banco: um admin rebaixado perde o acesso mesmo com uma sessão antiga aberta.

Esconder botões é só uma ajuda visual. As regras permanecem ativas se alguém tentar chamar a API diretamente.

## 5. O que é o token CSRF

Como usamos cookies de sessão, mantemos a proteção CSRF ativa. Antes de POST, PUT ou DELETE, `api.ts` busca `GET /auth/csrf` e envia o token no cabeçalho indicado pelo servidor. Isso também vale para login, cadastro e logout.

O token ajuda a bloquear requisições forjadas por outra página que tentem usar a sessão do navegador. Ele não substitui a autenticação nem a verificação do dono do chamado.

## 6. O que mudou na interface

- Login, cadastro público, identificação do perfil e botão **Sair**.
- Pessoas e filtro por solicitante aparecem apenas para administradores.
- Usuários abrem seus chamados em modo de consulta e criam novos em seu próprio nome.
- Admins escolhem qualquer status ao criar ou editar, inclusive fechado e concluído.
- A linha inteira da tabela e todo o cartão abrem o painel. Os botões continuam acessíveis pelo teclado e o botão de abertura ficou maior.

## 7. Dados existentes e configuração

O Hibernate continua com `ddl-auto=update`. A nova coluna `perfil` tem padrão `USUARIO`, para que contas existentes não ganhem administração automaticamente. Nenhum cargo antigo concede admin. Usuários cadastrados anteriormente com o encoder deste projeto podem usar a senha que cadastraram.

O frontend usa `/api` e o proxy encaminha para `http://localhost:8080`. Login e API devem permanecer no mesmo endereço de origem para os cookies funcionarem. Para publicar com HTTPS, configure `SESSION_COOKIE_SECURE=true` e encaminhe `/api` também no servidor de hospedagem. Não coloque senhas em variáveis `VITE_`, pois elas ficam públicas no navegador.

## 8. Como verificar e estudar

No backend:

```powershell
cd backend
.\mvnw.cmd test
```

Os testes usam `application-test.properties` e H2 em memória, com dados fictícios. Cobrem login, logout, CSRF real, troca do ID da sessão, cadastro sem elevação de privilégio, bloqueios entre pessoas, administração e alteração direta de status.

No frontend:

```powershell
cd frontend
npm.cmd run build
```

O build valida TypeScript e gera os arquivos da interface; não substitui testes de interação.

Para acompanhar a lógica, coloque breakpoints em `AuthController.cadastrar`, `UsuarioService.cadastrar` e `ChamadoService.listar`. Cadastre uma pessoa, entre e abra um chamado. Observe como o ID da sessão é usado na consulta. Depois entre com um admin e compare o caminho executado.

Referências oficiais: [autenticação com usuário e senha](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html), [sessões](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/session-management.html) e [CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).
