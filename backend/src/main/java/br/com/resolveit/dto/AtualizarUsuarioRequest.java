package br.com.resolveit.dto;

import br.com.resolveit.enums.SetorUsuario;
import br.com.resolveit.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarUsuarioRequest(@NotBlank(message = "Informe o nome.")
                                      String nome,
                                      @Email @NotBlank(message = "Informe o email.")
                                      String email,
                                      @NotNull(message = "Informe o setor.")
                                      SetorUsuario setor,
                                      @NotNull PerfilUsuario perfil) {
}
