package br.com.resolveit.controller;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.ChamadoResponse;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.dto.UsuarioResponse;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.service.ChamadoService;
import br.com.resolveit.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {
    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void criar(@Valid @RequestBody CriarChamadoRequest dados) {
        chamadoService.criar(dados);
    }

    @GetMapping
    public List<ChamadoResponse> listar() {
        return chamadoService.listarTodos().stream()
                .map(chamado -> {
                    Usuario usuario = chamado.getSolicitante();
                    UsuarioResponse dadosUsuario = new UsuarioResponse(
                            usuario.getId(),
                            usuario.getNome(),
                            usuario.getEmail(),
                            usuario.getSetor(),
                            usuario.getCargo()
                    );

                    return new ChamadoResponse(
                            chamado.getId(),
                            chamado.getTitulo(),
                            chamado.getDescricao(),
                            chamado.getStatus(),
                            dadosUsuario
                    );
                })
                .toList();
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscarPorId(@PathVariable("id") Long id) {
        Chamado chamado = chamadoService.buscarPorId(id);
        Usuario solicitante = chamado.getSolicitante();

        UsuarioResponse dadosUsuario = new UsuarioResponse(
                solicitante.getId(),
                solicitante.getNome(),
                solicitante.getEmail(),
                solicitante.getSetor(),
                solicitante.getCargo()
        );

        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus(),
                dadosUsuario
        );
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable("id") Long id,
                                     @Valid @RequestBody AtualizarChamadoRequest dados) {
        Chamado chamadoExistente = chamadoService.atualizar(id, dados);
        Usuario dadosUsuario = chamadoExistente.getSolicitante();

        UsuarioResponse usuario = new UsuarioResponse(
                dadosUsuario.getId(),
                dadosUsuario.getNome(),
                dadosUsuario.getEmail(),
                dadosUsuario.getSetor(),
                dadosUsuario.getCargo()
        );

        return new ChamadoResponse(
                chamadoExistente.getId(),
                chamadoExistente.getTitulo(),
                chamadoExistente.getDescricao(),
                chamadoExistente.getStatus(),
                usuario
        );
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable("id") Long id) {
        chamadoService.deletar(id);
    }

    @GetMapping
    public List<ChamadoResponse> buscarPorStatus(@RequestParam(value = "status", required = false) StatusChamado status) {
        return chamadoService.buscarPorStatus(status).stream()
                .map(dados -> new ChamadoResponse(
                        dados.getId(),
                        dados.getTitulo(),
                        dados.getDescricao(),
                        dados.getStatus(),
                        dados.getSolicitante()
                ))
                .toList();
    }
}
