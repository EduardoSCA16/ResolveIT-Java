package br.com.resolveit.model;

import br.com.resolveit.enums.Cargo;
import br.com.resolveit.enums.Setor;

public class Usuario {
    private  String nome;
    private String email;
    private String senha;
    private Setor setor;
    private Cargo cargo;

    // Constructor
    public Usuario() {}
    public Usuario(String nome, String email, String senha, Setor setor, Cargo cargo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.setor = setor;
        this.cargo = cargo;
    }

    // Getters e Setters
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

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }
}
