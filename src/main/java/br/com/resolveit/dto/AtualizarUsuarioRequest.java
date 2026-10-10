package br.com.resolveit.dto;

import br.com.resolveit.enums.SetorUsuario;

public record AtualizarUsuarioRequest(String nome,
                                      String email,
                                      SetorUsuario setor) {
}
