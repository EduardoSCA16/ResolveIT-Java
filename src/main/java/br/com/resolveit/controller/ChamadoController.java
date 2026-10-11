package br.com.resolveit.controller;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.ChamadoResponse;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.service.ChamadoService;
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
    public void criar(@RequestBody CriarChamadoRequest dados) {
        Chamado chamado = new Chamado(
                dados.titulo(),
                dados.descricao(),
                dados.status(),
                dados.usuario()
        );
        chamadoService.criar(chamado);
    }

    @GetMapping
    public List<ChamadoResponse> listar() {
        return chamadoService.listarTodos().stream()
                .map(chamado -> new ChamadoResponse(
                        chamado.getId(),
                        chamado.getTitulo(),
                        chamado.getDescricao(),
                        chamado.getStatus(),
                        chamado.getSolicitante()
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscarPorId(@PathVariable("id") Long id) {
        Chamado chamado = chamadoService.buscarPorId(id);

        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus(),
                chamado.getSolicitante()
        );
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable("id") Long id,
                                     @RequestBody AtualizarChamadoRequest dados) {
        Chamado chamadoExistente = chamadoService.atualizar(id, dados);

        return new ChamadoResponse(
                chamadoExistente.getId(),
                chamadoExistente.getTitulo(),
                chamadoExistente.getDescricao(),
                chamadoExistente.getStatus(),
                chamadoExistente.getSolicitante()
        );
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable("id") Long id) {
        chamadoService.deletar(id);
    }
}
