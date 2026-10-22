package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarChamadoRequest(@NotBlank(message = "Informe o título.")
                                      String titulo,
                                      String descricao,
                                      @NotNull(message = "Informe o status.")
                                      StatusChamado status) {
}
