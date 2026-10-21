# ResolveIT

Projeto pessoal de gerenciamento de chamados de suporte, feito com Java, Spring Boot, Spring Data JPA e PostgreSQL.

## Organização

```text
ResolveIT-Java/
├── backend/     # API Java/Spring Boot e Maven Wrapper
└── frontend/    # Interface React e TypeScript
```

## Backend

Na IDE, importe o projeto Maven pelo arquivo `backend/pom.xml` e execute a classe `ResolveitApplication`. Use a configuração existente do PostgreSQL e suas variáveis de ambiente.

Também é possível iniciar pelo terminal, a partir da raiz deste repositório, com o JDK 25 configurado:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Aguarde o Spring Boot iniciar em `http://localhost:8080`.

## Frontend

A interface fica em `frontend/` e usa React, TypeScript, Vite, Motion e Lucide, com fontes incluídas no projeto.

Com o backend em execução, abra outro terminal na raiz deste repositório:

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha as chamadas de `/api` para o backend local.

Veja o [guia do frontend](frontend/README.md) para entender a integração, as variáveis de ambiente e a organização dos arquivos.
