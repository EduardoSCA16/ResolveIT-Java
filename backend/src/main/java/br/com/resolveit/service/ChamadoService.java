package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.ChamadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Page<Chamado> listar(StatusChamado status, Long solicitanteId, Pageable pageable) {
        if(status != null && solicitanteId != null) {
            return chamadoRepository.findByStatusAndSolicitanteId(status, solicitanteId, pageable);
        }

        if(status != null) {
            return chamadoRepository.findByStatus(status, pageable);
        }

        if(solicitanteId != null) {
            return chamadoRepository.findBySolicitanteId(solicitanteId, pageable);
        }

        return chamadoRepository.findAll(pageable);
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

        if(statusChamado == StatusChamado.FECHADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chamado já está fechado."
            );
        }
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
