package br.com.resolveit.dto;

import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.enums.SetorUsuario;
import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.model.Usuario;

public record UsuarioResponse(Long id,
                              String nome,
                              String email,
                              SetorUsuario setor,
                              CargoUsuario cargo,
                              PerfilUsuario perfil) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getSetor(), usuario.getCargo(), usuario.getPerfil());
    }
}
