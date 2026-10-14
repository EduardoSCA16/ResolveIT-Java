package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.ChamadoResponse;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.repository.ChamadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChamadoService {
    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;

    public ChamadoService(ChamadoRepository chamadoRepository, UsuarioService usuarioService) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioService = usuarioService;
    }

    public void criar(Chamado chamado) {
        chamadoRepository.save(chamado);
    }

    public List<Chamado> listarTodos() {
        return chamadoRepository.findAll();
    }

    public Chamado buscarPorId(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chamado não encontrado."
                ));
    }

    public Chamado atualizar(Long id, AtualizarChamadoRequest dados) {
        Chamado chamadoExistente = buscarPorId(id);
        chamadoExistente.setTitulo(dados.titulo());
        chamadoExistente.setDescricao(dados.descricao());
        chamadoExistente.setStatus(dados.status());
        chamadoExistente.setSolicitante(usuarioService.buscarPorId(dados.usuarioId()));

        return chamadoRepository.save(chamadoExistente);
    }

    public void deletar(Long id) {
        Chamado chamado = buscarPorId(id);
        chamadoRepository.delete(chamado);
    }
}
