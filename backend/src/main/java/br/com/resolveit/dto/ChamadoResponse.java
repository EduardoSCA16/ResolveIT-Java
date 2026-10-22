package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;

public record ChamadoResponse(Long id,
                              String titulo,
                              String descricao,
                              StatusChamado status,
                              UsuarioResponse usuario) {
    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
                chamado.getStatus(), UsuarioResponse.de(chamado.getSolicitante()));
    }
}
