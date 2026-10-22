package br.com.resolveit;

import br.com.resolveit.config.AdminInicialConfig;
import br.com.resolveit.enums.*;
import br.com.resolveit.model.*;
import br.com.resolveit.repository.*;
import br.com.resolveit.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ChamadoRepository chamados;
    @Autowired UsuarioService service;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    Usuario ana, bia, admin;
    Chamado chamadoAna, chamadoBia;
    static final String SENHA = "SenhaDeTeste123!";

    @BeforeEach
    void prepararBancoIsolado() {
        chamados.deleteAll();
        usuarios.deleteAll();
        ana = criarUsuario("Ana", "ana@example.test", PerfilUsuario.USUARIO);
        bia = criarUsuario("Bia", "bia@example.test", PerfilUsuario.USUARIO);
        admin = criarUsuario("Admin", "admin@example.test", PerfilUsuario.ADMIN);
        chamadoAna = chamados.save(new Chamado("Computador da Ana", "Detalhes da Ana", StatusChamado.ABERTO, ana));
        chamadoBia = chamados.save(new Chamado("Computador da Bia", "Detalhes da Bia", StatusChamado.FECHADO, bia));
    }

    Usuario criarUsuario(String nome, String email, PerfilUsuario perfil) {
        var usuario = new Usuario(nome, email, encoder.encode(SENHA), SetorUsuario.TI, CargoUsuario.SOLICITANTE);
        usuario.setPerfil(perfil);
        return usuarios.save(usuario);
    }

    MockHttpSession entrar(Usuario usuario) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").with(csrf())
                .param("email", usuario.getEmail()).param("senha", SENHA))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    @Test
    void anonimoNaoConsultaDados() throws Exception {
        mvc.perform(get("/chamados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroPublicoNuncaConcedeAdminESalvaHash() throws Exception {
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Nova Pessoa","email":"NOVA@example.test","senha":"SenhaNova123!","setor":"TI","perfil":"ADMIN","cargo":"TECNICO"}
                """)).andExpect(status().isCreated());
        var nova = usuarios.findByEmailIgnoreCase("nova@example.test").orElseThrow();
        assertThat(nova.getPerfil()).isEqualTo(PerfilUsuario.USUARIO);
        assertThat(nova.getCargo()).isEqualTo(CargoUsuario.SOLICITANTE);
        assertThat(nova.getEmail()).isEqualTo("nova@example.test");
        assertThat(nova.getSenha()).isNotEqualTo("SenhaNova123!");
        assertThat(encoder.matches("SenhaNova123!", nova.getSenha())).isTrue();
    }

    @Test
    void validaSenhaEEmailDuplicado() throws Exception {
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Nova","email":"nova@example.test","senha":"123","setor":"TI"}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Outra Ana","email":"ANA@example.test","senha":"SenhaNova123!","setor":"TI"}
                """)).andExpect(status().isConflict());
    }

    @Test
    void loginInvalidoNaoCriaSessaoAutenticada() throws Exception {
        mvc.perform(post("/auth/login").with(csrf()).param("email", ana.getEmail()).param("senha", "errada"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginMantemSessaoESaidaInvalida() throws Exception {
        var session = entrar(ana);
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ana.getId())).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(post("/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSoVeOsPropriosMesmoManipulandoFiltro() throws Exception {
        var session = entrar(ana);
        mvc.perform(get("/chamados").session(session).param("solicitanteId", bia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(chamadoAna.getId()));
        mvc.perform(get("/chamados").session(session).param("status", "FECHADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/chamados/{id}", chamadoBia.getId()).session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/chamados/{id}", chamadoAna.getId()).session(session)).andExpect(status().isOk());
    }

    @Test
    void usuarioCriaSomenteEmSeuNomeEEmAberto() throws Exception {
        var session = entrar(ana);
        mvc.perform(post("/chamados").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Novo pedido\"}")).andExpect(status().isCreated());
        var novo = chamados.findAll().stream().filter(c -> c.getTitulo().equals("Novo pedido")).findFirst().orElseThrow();
        assertThat(novo.getSolicitante().getId()).isEqualTo(ana.getId());
        assertThat(novo.getStatus()).isEqualTo(StatusChamado.ABERTO);
        mvc.perform(post("/chamados").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Forjado\",\"usuarioId\":" + bia.getId() + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/chamados").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Forjado\",\"status\":\"FECHADO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNaoAlteraNemExcluiChamadosNemGerenciaPessoas() throws Exception {
        var session = entrar(ana);
        for (var chamado : new Chamado[]{chamadoAna, chamadoBia}) {
            mvc.perform(put("/chamados/{id}", chamado.getId()).session(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Alterado\",\"status\":\"FECHADO\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/chamados/{id}", chamado.getId()).session(session).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/usuarios").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/{id}", bia.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Forjado","email":"forjado@example.test","senha":"SenhaNova123!","setor":"TI","perfil":"ADMIN"}
                """)).andExpect(status().isForbidden());
        mvc.perform(put("/usuarios/{id}", ana.getId()).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Ana","email":"ana@example.test","setor":"TI","perfil":"ADMIN"}
                """)).andExpect(status().isForbidden());
        mvc.perform(delete("/usuarios/{id}", bia.getId()).session(session).with(csrf())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONCLUIDO", "FECHADO", "EM_ANDAMENTO"})
    void adminPodePularDiretoParaQualquerStatus(String status) throws Exception {
        mvc.perform(put("/chamados/{id}", chamadoAna.getId()).session(entrar(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Atendido\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
        assertThat(chamados.findById(chamadoAna.getId()).orElseThrow().getStatus().name()).isEqualTo(status);
    }

    @Test
    void adminConsultaTodosCriaFechadoReabreEExclui() throws Exception {
        var session = entrar(admin);
        mvc.perform(get("/chamados").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/usuarios").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].senha").doesNotExist());
        mvc.perform(post("/chamados").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Resolvido\",\"usuarioId\":" + bia.getId() + ",\"status\":\"FECHADO\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/chamados/{id}", chamadoBia.getId()).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Reaberto\",\"status\":\"ABERTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTO"));
        mvc.perform(delete("/chamados/{id}", chamadoBia.getId()).session(session).with(csrf())).andExpect(status().isOk());
        assertThat(chamados.existsById(chamadoBia.getId())).isFalse();
    }

    @Test
    void adminCriaOutroAdminMasNaoRemoveProprioAcesso() throws Exception {
        var session = entrar(admin);
        mvc.perform(post("/usuarios").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Admin 2","email":"admin2@example.test","senha":"SenhaNova123!","setor":"TI","perfil":"ADMIN"}
                """)).andExpect(status().isCreated());
        assertThat(usuarios.findByEmailIgnoreCase("admin2@example.test").orElseThrow().getPerfil()).isEqualTo(PerfilUsuario.ADMIN);
        mvc.perform(put("/usuarios/{id}", admin.getId()).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Admin","email":"admin@example.test","setor":"TI","perfil":"USUARIO"}
                """)).andExpect(status().isConflict());
        mvc.perform(delete("/usuarios/{id}", admin.getId()).session(session).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/usuarios/{id}", ana.getId()).session(session).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void rebaixarAdminRevogaPoderesDaSessaoJaAberta() throws Exception {
        var segundo = criarUsuario("Admin 2", "admin2@example.test", PerfilUsuario.ADMIN);
        var sessaoAntiga = entrar(segundo);
        mvc.perform(put("/usuarios/{id}", segundo.getId()).session(entrar(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Admin 2","email":"admin2@example.test","setor":"TI","perfil":"USUARIO"}
                """)).andExpect(status().isOk());
        mvc.perform(get("/usuarios").session(sessaoAntiga)).andExpect(status().isForbidden());
        mvc.perform(get("/chamados").session(sessaoAntiga)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void csrfObrigatorioNoCadastroLoginMutacoesESaida() throws Exception {
        mvc.perform(post("/auth/login").param("email", ana.getEmail()).param("senha", SENHA)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        var session = entrar(admin);
        mvc.perform(delete("/chamados/{id}", chamadoAna.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void fluxoRealCsrfRotacaoDeSessaoELogout() throws Exception {
        var result = mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        String idAntes = session.getId();
        JsonNode csrfToken = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/auth/login").session(session)
                .header(csrfToken.get("headerName").asText(), csrfToken.get("token").asText())
                .param("email", "ANA@example.test").param("senha", SENHA)).andExpect(status().isNoContent());
        assertThat(session.getId()).isNotEqualTo(idAntes);
        var novoToken = json.readTree(mvc.perform(get("/auth/csrf").session(session)).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/chamados").session(session)
                .header(novoToken.get("headerName").asText(), novoToken.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Via sessão real\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/logout").session(session)
                .header(novoToken.get("headerName").asText(), novoToken.get("token").asText()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void bootstrapCriaAdminUmaVezESemPromoverContaPublica() {
        var bootstrap = new AdminInicialConfig(usuarios, service, "inicial@example.test", SENHA);
        bootstrap.run(new DefaultApplicationArguments());
        var inicial = usuarios.findByEmailIgnoreCase("inicial@example.test").orElseThrow();
        assertThat(inicial.getPerfil()).isEqualTo(PerfilUsuario.ADMIN);
        String hash = inicial.getSenha();
        bootstrap.run(new DefaultApplicationArguments());
        assertThat(usuarios.findById(inicial.getId()).orElseThrow().getSenha()).isEqualTo(hash);
        assertThatThrownBy(() -> new AdminInicialConfig(usuarios, service, ana.getEmail(), SENHA)
                .run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.findById(ana.getId()).orElseThrow().getPerfil()).isEqualTo(PerfilUsuario.USUARIO);
    }
}
