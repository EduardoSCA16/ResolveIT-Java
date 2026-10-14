package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Usuario;

public record ChamadoResponse(Long id,
                              String titulo,
                              String descricao,
                              StatusChamado status,
                              Long usuarioId) {
}
