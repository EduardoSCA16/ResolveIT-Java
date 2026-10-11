package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Usuario;

public record CriarChamadoRequest(String titulo,
                                  String descricao,
                                  StatusChamado status,
                                  Usuario usuario) {
}
