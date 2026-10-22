package br.com.resolveit.dto;

import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.enums.SetorUsuario;
import jakarta.validation.constraints.*;

// Este DTO só é aceito na área administrativa, nunca no cadastro público.
public record CadastroAdminRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String senha,
        @NotNull SetorUsuario setor,
        @NotNull PerfilUsuario perfil) {
}
