package br.com.resolveit.dto;

import br.com.resolveit.enums.SetorUsuario;

public record CadastroUsuarioRequest(String nome,
                                     String email,
                                     String senha,
                                     SetorUsuario setor) {
}
