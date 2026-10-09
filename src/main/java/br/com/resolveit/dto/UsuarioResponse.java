package br.com.resolveit.dto;

import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.enums.SetorUsuario;

public record UsuarioResponse(Long id,
                              String nome,
                              String email,
                              SetorUsuario setor,
                              CargoUsuario cargo) {
}
