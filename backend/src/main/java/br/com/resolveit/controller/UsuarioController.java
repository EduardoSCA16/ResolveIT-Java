package br.com.resolveit.controller;

import br.com.resolveit.dto.*;
import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
@PreAuthorize("@usuarioAutenticado.isAdmin()")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@Valid @RequestBody CadastroAdminRequest dados) {
        var usuario = new Usuario(dados.nome(), dados.email(), dados.senha(), dados.setor(), CargoUsuario.SOLICITANTE);
        usuario.setPerfil(dados.perfil());
        service.cadastrar(usuario);
    }

    @GetMapping
    public List<UsuarioResponse> listar() { return service.listarTodos().stream().map(UsuarioResponse::de).toList(); }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable("id") Long id) { return UsuarioResponse.de(service.buscarPorId(id)); }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarUsuarioRequest dados) {
        return UsuarioResponse.de(service.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable("id") Long id) { service.deletar(id); }
}
