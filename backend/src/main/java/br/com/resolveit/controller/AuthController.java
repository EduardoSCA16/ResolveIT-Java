package br.com.resolveit.controller;

import br.com.resolveit.dto.CadastroUsuarioRequest;
import br.com.resolveit.dto.UsuarioResponse;
import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.service.UsuarioAutenticado;
import br.com.resolveit.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarios;
    private final UsuarioAutenticado autenticado;

    public AuthController(UsuarioService usuarios, UsuarioAutenticado autenticado) {
        this.usuarios = usuarios;
        this.autenticado = autenticado;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }

    @GetMapping("/me")
    public UsuarioResponse me() { return UsuarioResponse.de(autenticado.atual()); }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@Valid @RequestBody CadastroUsuarioRequest dados) {
        // O perfil nunca vem do navegador: todo cadastro público começa como USUARIO.
        usuarios.cadastrar(new Usuario(dados.nome(), dados.email(), dados.senha(),
                dados.setor(), CargoUsuario.SOLICITANTE));
    }
}
