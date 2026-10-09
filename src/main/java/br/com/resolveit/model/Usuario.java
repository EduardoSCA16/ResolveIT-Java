package br.com.resolveit.model;

import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.enums.SetorUsuario;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Column(unique = true)
    private String email;
    private String senha;
    @Enumerated(EnumType.STRING)
    private SetorUsuario setor;
    @Enumerated(EnumType.STRING)
    private CargoUsuario cargo;

    // Constructor
    public Usuario() {}
    public Usuario(String nome, String email, String senha, SetorUsuario setor, CargoUsuario cargo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.setor = setor;
        this.cargo = cargo;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public SetorUsuario getSetor() {
        return setor;
    }

    public void setSetor(SetorUsuario setor) {
        this.setor = setor;
    }

    public CargoUsuario getCargo() {
        return cargo;
    }

    public void setCargo(CargoUsuario cargo) {
        this.cargo = cargo;
    }
}
