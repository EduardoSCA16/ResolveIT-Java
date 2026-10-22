package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarChamadoRequest;
import br.com.resolveit.dto.CriarChamadoRequest;
import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.model.Chamado;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.ChamadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoService {
    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioAutenticado autenticado;

    public ChamadoService(ChamadoRepository chamadoRepository, UsuarioService usuarioService, UsuarioAutenticado autenticado) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioService = usuarioService;
        this.autenticado = autenticado;
    }

    @Transactional
    public void criar(CriarChamadoRequest dados) {
        Usuario atual = autenticado.atual();
        boolean admin = atual.getPerfil() == PerfilUsuario.ADMIN;
        if (!admin && dados.usuarioId() != null && !dados.usuarioId().equals(atual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode abrir chamados em seu nome.");
        }
        if (!admin && dados.status() != null && dados.status() != StatusChamado.ABERTO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores definem o status.");
        }
        Usuario solicitante = admin && dados.usuarioId() != null
                ? usuarioService.buscarPorId(dados.usuarioId()) : atual;
        Chamado chamado = new Chamado(
                dados.titulo(),
                dados.descricao(),
                admin && dados.status() != null ? dados.status() : StatusChamado.ABERTO,
                solicitante
        );
        chamadoRepository.save(chamado);
    }

    public Page<Chamado> listar(StatusChamado status, Long solicitanteId, Pageable pageable) {
        Usuario atual = autenticado.atual();
        // O filtro é imposto no servidor, mesmo se alguém manipular a URL no navegador.
        if (atual.getPerfil() != PerfilUsuario.ADMIN) solicitanteId = atual.getId();
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
        Usuario atual = autenticado.atual();
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chamado não encontrado."
                ));
        if (atual.getPerfil() != PerfilUsuario.ADMIN && !chamado.getSolicitante().getId().equals(atual.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado.");
        }
        return chamado;
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    @Transactional
    public Chamado atualizar(Long id, AtualizarChamadoRequest dados) {
        Chamado chamadoExistente = buscarPorId(id);

        chamadoExistente.setTitulo(dados.titulo());
        chamadoExistente.setDescricao(dados.descricao());
        chamadoExistente.setStatus(dados.status());
        return chamadoRepository.save(chamadoExistente);
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    @Transactional
    public void deletar(Long id) {
        Chamado chamado = buscarPorId(id);
        chamadoRepository.delete(chamado);
    }
}
