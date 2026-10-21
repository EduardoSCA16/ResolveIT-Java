# ResolveIT — frontend

Interface em React e TypeScript para o backend Java/Spring Boot deste repositório. O Vite prepara o código para o navegador e oferece o servidor de desenvolvimento.

A interface consulta os dados da API: não cria usuários ou chamados automaticamente e não usa uma base de demonstração. Para utilizá-la, o backend e seu banco precisam estar em execução.

## Executar localmente

O ambiente foi preparado com Node.js 24 e npm. O Vite 8.3.5 instalado declara o requisito `^20.19.0 || >=22.12.0`; Node.js 24 atende a esse requisito.

1. Pela IDE, importe o projeto Maven pelo arquivo `backend/pom.xml`, a partir da raiz do repositório, e execute a classe Java `ResolveitApplication`. Use a configuração existente do PostgreSQL e suas variáveis de ambiente. Aguarde o Spring Boot iniciar em `http://localhost:8080`.
2. Abra outro terminal na pasta `frontend` e instale as dependências:

   ```powershell
   npm install
   ```

3. Inicie o frontend:

   ```powershell
   npm run dev
   ```

4. Abra [http://localhost:5173](http://localhost:5173). Mantenha os dois processos em execução enquanto usa a interface.

Se o terminal estiver na raiz do repositório, entre na pasta com `cd frontend` antes dos comandos npm. Se a porta 5173 estiver ocupada, o Vite informa o problema no terminal.

Para verificar os tipos e gerar os arquivos de distribuição:

```powershell
npm run build
```

O resultado fica em `frontend/dist`. Para visualizar esse resultado localmente, execute `npm run preview` e abra [http://localhost:4173](http://localhost:4173). Essa visualização também precisa do backend em execução. O comando `build` verifica TypeScript e empacota a interface; ele não executa testes automatizados.

## Como a integração funciona

No desenvolvimento, o navegador chama o mesmo endereço que serve a interface. O proxy configurado no Vite encaminha as chamadas da API para o Spring:

```text
Navegador: http://localhost:5173
    ↓ GET /api/chamados?page=0&size=10
Proxy do Vite: remove o prefixo /api
    ↓ GET http://localhost:8080/chamados?page=0&size=10
Controller → Service → Repository → PostgreSQL
    ↑ DTOs em JSON
Interface atualiza a lista
```

O backend já recebe `/chamados`; o prefixo `/api` pertence à configuração do frontend. Nesse fluxo, não é necessário adicionar uma configuração global de CORS ao backend.

O Java continua responsável por consultar e persistir dados, validar as entradas e aplicar as regras de negócio. O cliente `fetch` do frontend envia as requisições e interpreta as respostas. Node.js executa as ferramentas do frontend, enquanto o código React gerado é executado no navegador.

O proxy é uma configuração do Vite para desenvolvimento e visualização local. Ao hospedar a pasta `dist` em outro servidor, será necessário configurar o encaminhamento para a API nesse ambiente.

### Alterar o endereço do backend

Os valores padrão já atendem ao backend em `http://localhost:8080`. Só é necessário criar uma configuração local se esse endereço mudar.

Na pasta `frontend`, copie o arquivo de exemplo:

```powershell
Copy-Item .env.example .env.local
```

O arquivo contém:

```dotenv
VITE_API_BASE_URL=/api
API_TARGET=http://localhost:8080
```

- `API_TARGET`: destino do proxy, lido pelo Vite. Se o backend passar a usar outra porta ou máquina, altere esse valor e mantenha `VITE_API_BASE_URL=/api` para continuar usando o proxy.
- `VITE_API_BASE_URL`: prefixo usado pelo navegador. Variáveis que começam com `VITE_` ficam públicas no código da interface; não coloque senhas, tokens privados ou credenciais do banco nelas.

Reinicie `npm run dev` depois de alterar `.env.local`. Para mudar somente o destino do frontend, não é preciso editar `application.properties`.

## O que a interface oferece

- **Visão geral:** acompanhamento de chamados e pessoas a partir dos dados da API.
- **Chamados:** consulta, criação, edição, exclusão com confirmação, filtros por status e solicitante, ordenação e paginação.
- **Pessoas:** consulta, cadastro, edição e exclusão de usuários, respeitando a regra do backend que impede excluir quem possui chamados.
- **Formulários:** indicação de campos obrigatórios, validação de e-mail, mensagens de erro e estado de envio para evitar cliques repetidos.
- **Status:** manutenção do status atual ou avanço de uma etapa, seguindo `ABERTO → EM_ANDAMENTO → CONCLUIDO → FECHADO`. Chamados fechados ficam disponíveis para consulta e têm a edição bloqueada.

O backend ainda não possui login ou controle de acesso. O frontend consome os endpoints disponíveis e não acrescenta autenticação. A senha do cadastro de usuário é enviada à API, mas não aparece nas respostas de consulta.

## Onde estudar o código

| Arquivo | Responsabilidade |
| --- | --- |
| `src/App.tsx` | Telas, navegação, carregamento dos dados, filtros e paginação. |
| `src/types.ts` | Contratos TypeScript dos DTOs, payloads de formulário, filtros e rótulos dos enums. |
| `src/lib/api.ts` | Chamadas `fetch`, montagem dos parâmetros e conversão de erros em `ApiError`. |
| `src/components/RecordDrawer.tsx` | Painel de criação, edição e exclusão de chamados e usuários. |
| `src/styles.css` | Cores, tipografia, layout e adaptação para telas menores. |
| `vite.config.ts` | React, portas locais e proxy de `/api` para o Spring Boot. |

Uma sequência de estudo é acompanhar um clique em `App.tsx`, localizar a função chamada em `api.ts`, observar a requisição no navegador e abrir o controller Java correspondente. Depois, siga o service e o repository para entender como a resposta é produzida.

### Exemplo: buscar uma página

O cliente recebe os filtros e usa `URLSearchParams` para montar a URL:

```ts
const pagina = await listTickets({
  status: 'ABERTO',
  page: 0,
  size: 10,
  sort: 'id,desc',
})

console.log(pagina.content)
console.log(pagina.page.totalElements)
```

Isso envia um `GET /api/chamados?status=ABERTO&page=0&size=10&sort=id%2Cdesc`. A vírgula codificada como `%2C` chega ao backend como parte de `id,desc`. O índice da primeira página é `0`, mesmo que a interface a apresente como página 1.

O controller retorna um `PagedModel<ChamadoResponse>`. Uma consulta sem resultados tem esta estrutura:

```json
{
  "content": [],
  "page": {
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "number": 0
  }
}
```

Com resultados, cada item de `content` contém `id`, `titulo`, `descricao`, `status` e o DTO `usuario`. `page` informa o tamanho solicitado, a quantidade total de registros, a quantidade total de páginas e o índice atual. A lista de usuários, por sua vez, vem de `GET /api/usuarios` como um array simples.

### Exemplo: criar um chamado

O formulário fornece um título, uma descrição opcional e o ID de um usuário existente:

```ts
await createTicket({
  titulo: titulo.trim(),
  descricao: descricao.trim(),
  usuarioId: Number(usuarioId),
})
```

Essa função envia `POST /api/chamados` com um corpo JSON contendo `titulo`, `descricao` e `usuarioId`. O status inicial é definido como `ABERTO` pelo backend.

O método Java retorna `void` com HTTP `201 Created`, portanto não há um objeto JSON para ler nesse sucesso. O cliente aceita a resposta vazia, e a interface consulta os dados novamente após salvar. As exclusões também podem responder sem corpo, com HTTP 200.

### Erros e cancelamento

Erros de validação HTTP 400 podem trazer um array de objetos `{ "campo": "...", "mensagem": "..." }`; o formulário usa esses campos para indicar o que corrigir. O cliente também interpreta mensagens públicas nos formatos ProblemDetail e Spring Boot, e apresenta uma mensagem genérica para falhas 500 sem mostrar rastros de exceções.

As funções de listagem aceitam `AbortSignal`. Isso permite cancelar uma consulta que ficou desatualizada quando o usuário troca de filtro ou sai da tela. Um cancelamento não é convertido em erro de conexão.

## Observar pelo navegador

1. Abra as ferramentas do navegador com `F12` e selecione **Network/Rede**.
2. Filtre por **Fetch/XHR** e altere um filtro da tela de chamados.
3. Selecione a requisição de `chamados`. Em **Headers/Cabeçalhos**, confira a URL e os parâmetros de consulta; em **Response/Resposta**, veja `content` e `page`.
4. Ao salvar um formulário, observe o método `POST` ou `PUT` e a seção **Payload/Corpo da solicitação**. Compare os nomes dos campos com os records Java de entrada.
5. Confira o código HTTP: 200 em consultas, 201 em criações, 400 em validações e 409 em conflitos de regra de negócio.

Para chamadas `GET`, os filtros ficam na URL. Para `POST` e `PUT`, os dados do formulário ficam no corpo JSON. O caminho visto na aba Network começa com `/api`, enquanto o controller Java recebe o caminho após a remoção desse prefixo pelo proxy.

## Roteiro de verificação manual

Este roteiro pode ser seguido com os registros existentes na sua base; ele não representa testes já executados. Se a base estiver vazia, cadastre primeiro uma pessoa e depois seus chamados pela interface.

1. **Quatro combinações de filtros:** consulte sem filtros, somente com status, somente com solicitante e com ambos. Com os dois preenchidos, cada resultado precisa atender às duas condições. Sem correspondências, a página deve mostrar o estado vazio.
2. **Paginação:** com mais registros do que o tamanho de página, avance e volte. Confira `page=0`, `page=1`, o conteúdo retornado e os totais na aba Network. Alterar filtros deve levar de volta à primeira página.
3. **Cadastro e atualização:** crie e edite uma pessoa; crie um chamado para ela e avance seu status uma etapa por vez. Depois de salvar, confira os dados atualizados na listagem.
4. **Validação:** tente enviar campos obrigatórios vazios e um e-mail inválido. O navegador ou o formulário pode interromper o envio antes da requisição; os erros retornados pela API também devem aparecer no formulário.
5. **Conflito de e-mail:** tente cadastrar um e-mail já utilizado ou trocar o e-mail de uma pessoa pelo de outra. A API deve retornar 409 e a interface deve exibir o erro.
6. **Chamado fechado:** leve um chamado até `FECHADO` e abra seus detalhes. A edição deve ficar bloqueada na interface. Para conferir a proteção do backend, um `PUT /chamados/{id}` enviado por um cliente HTTP a esse registro deve retornar 409, inclusive se mantiver `FECHADO`; a interface não oferece esse envio.
7. **Exclusão:** confirme a exclusão de um registro escolhido para essa verificação. Tentar excluir um usuário com chamados deve resultar em 409. A confirmação é necessária porque a exclusão persiste no banco.

Se surgir uma falha de conexão, confira os terminais do Vite e do Spring, a porta do backend e o destino do proxy. Uma lista vazia com resposta HTTP 200 é diferente de uma API indisponível: a interface deve apresentar o estado correspondente.
