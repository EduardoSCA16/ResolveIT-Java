package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.ChamadoResponse;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.model.Usuario;
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

    public void criar(CriarChamadoRequest dados) {
        Usuario solicitante = usuarioService.buscarPorId(dados.usuarioId());
        Chamado chamado = new Chamado(
                dados.titulo(),
                dados.descricao(),
                StatusChamado.ABERTO,
                solicitante
        );
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
        StatusChamado statusChamado = chamadoExistente.getStatus();
        StatusChamado novoStatus = dados.status();

        if(!statusChamado.podeMudarPara(novoStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mudança de Status não permitida."
            );
        }

        chamadoExistente.setTitulo(dados.titulo());
        chamadoExistente.setDescricao(dados.descricao());
        chamadoExistente.setStatus(dados.status());
        return chamadoRepository.save(chamadoExistente);
    }

    public void deletar(Long id) {
        Chamado chamado = buscarPorId(id);
        chamadoRepository.delete(chamado);
    }
}
