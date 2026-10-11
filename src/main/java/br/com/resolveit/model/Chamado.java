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

    // Constructor
    public Chamado() {}
    public Chamado(String titulo, String descricao, StatusChamado status, Usuario solicitante) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.status = status;
        this.solicitante = solicitante;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public void setStatus(StatusChamado status) {
        this.status = status;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }
}
