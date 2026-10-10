package br.com.resolveit.controller;

import br.com.resolveit.dto.AtualizarUsuarioRequest;
import br.com.resolveit.dto.CadastroUsuarioRequest;
import br.com.resolveit.dto.UsuarioResponse;
import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@RequestBody CadastroUsuarioRequest dados) {
        Usuario usuario = new Usuario(
                dados.nome(),
                dados.email(),
                dados.senha(),
                dados.setor(),
                CargoUsuario.SOLICITANTE
        );
        usuarioService.cadastrar(usuario);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos().stream()
                .map(usuario -> new UsuarioResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getSetor(),
                        usuario.getCargo()
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable("id") Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSetor(),
                usuario.getCargo()
        );
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizarUsuario(@PathVariable("id") Long id,
                                            @RequestBody AtualizarUsuarioRequest dados) {
        Usuario usuarioExistente = usuarioService.atualizarUsuario(id, dados);

        return new UsuarioResponse(
                usuarioExistente.getId(),
                usuarioExistente.getNome(),
                usuarioExistente.getEmail(),
                usuarioExistente.getSetor(),
                usuarioExistente.getCargo()
        );
    }
}
