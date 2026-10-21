package br.com.resolveit.controller;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.ChamadoResponse;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.dto.UsuarioResponse;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public PagedModel<ChamadoResponse> listar(
            @RequestParam(value = "status", required = false)
            StatusChamado status,

            @RequestParam(value = "solicitanteId", required = false)
            Long solicitanteId,

            @PageableDefault(size = 10, sort = "id")
            Pageable pageable) {
        Page<ChamadoResponse> pagina = chamadoService.listar(status, solicitanteId, pageable)
                .map(dados -> {
                    UsuarioResponse usuario = new UsuarioResponse(
                            dados.getSolicitante().getId(),
                            dados.getSolicitante().getNome(),
                            dados.getSolicitante().getEmail(),
                            dados.getSolicitante().getSetor(),
                            dados.getSolicitante().getCargo()
                    );

                    return  new ChamadoResponse(
                            dados.getId(),
                            dados.getTitulo(),
                            dados.getDescricao(),
                            dados.getStatus(),
                            usuario
                    );
                });

        return new PagedModel<>(pagina);
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
}
