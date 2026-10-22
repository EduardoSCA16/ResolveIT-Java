package br.com.resolveit.dto;

import br.com.resolveit.enums.StatusChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CriarChamadoRequest(@NotBlank(message = "Informe o título.")
                                  String titulo,
                                  String descricao,
                                  @Positive Long usuarioId,
                                  StatusChamado status) {
}
