package br.com.resolveit.dto;

import br.com.resolveit.enums.SetorUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CadastroUsuarioRequest(@NotBlank(message = "Informe o nome.")
                                     String nome,
                                     @Email @NotBlank(message = "Informe o email.")
                                     String email,
                                     @NotBlank(message = "Informe a senha.")
                                     String senha,
                                     @NotNull(message = "Informe o setor.")
                                     SetorUsuario setor) {
}
