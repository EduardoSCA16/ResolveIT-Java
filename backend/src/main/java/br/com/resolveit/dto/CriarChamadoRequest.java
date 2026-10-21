package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarChamadoRequest(@NotBlank(message = "Informe o título.")
                                  String titulo,
                                  String descricao,
                                  @Positive @NotNull(message = "Informe o usuário.")
                                  Long usuarioId) {
}
