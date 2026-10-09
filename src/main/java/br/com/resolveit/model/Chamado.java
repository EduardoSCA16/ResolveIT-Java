package br.com.resolveit.model;

import br.com.resolveit.enums.StatusChamado;
import jakarta.persistence.*;

@Entity
@Table(name = "chamados")
public class Chamado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descricao;
    @Enumerated(EnumType.STRING)
    private StatusChamado status;
    @ManyToOne
    private Usuario solicitante;
}
