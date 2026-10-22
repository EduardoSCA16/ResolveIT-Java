package br.com.resolveit.controller;

import br.com.resolveit.dto.*;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {
    private final ChamadoService service;

    public ChamadoController(ChamadoService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void criar(@Valid @RequestBody CriarChamadoRequest dados) { service.criar(dados); }

    @GetMapping
    public PagedModel<ChamadoResponse> listar(
            @RequestParam(value = "status", required = false) StatusChamado status,
            @RequestParam(value = "solicitanteId", required = false) Long solicitanteId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(service.listar(status, solicitanteId, pageable).map(ChamadoResponse::de));
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscarPorId(@PathVariable("id") Long id) {
        return ChamadoResponse.de(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarChamadoRequest dados) {
        return ChamadoResponse.de(service.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable("id") Long id) { service.deletar(id); }
}
