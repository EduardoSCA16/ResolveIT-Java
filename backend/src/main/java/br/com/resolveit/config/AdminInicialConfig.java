package br.com.resolveit.config;

import br.com.resolveit.enums.CargoUsuario;
import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.enums.SetorUsuario;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.UsuarioRepository;
import br.com.resolveit.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInicialConfig implements ApplicationRunner {
    private final UsuarioRepository repository;
    private final UsuarioService service;
    private final String email;
    private final String senha;

    public AdminInicialConfig(UsuarioRepository repository, UsuarioService service,
            @Value("${resolveit.admin.email:}") String email,
            @Value("${resolveit.admin.password:}") String senha) {
        this.repository = repository;
        this.service = service;
        this.email = email.trim();
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() && senha.isEmpty()) return;
        if (email.isEmpty() || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                || senha.isBlank() || senha.length() < 8 || senha.length() > 64) {
            throw new IllegalStateException("Configure RESOLVEIT_ADMIN_EMAIL e RESOLVEIT_ADMIN_PASSWORD (8 a 64 caracteres).");
        }
        var existente = repository.findByEmailIgnoreCase(email);
        if (existente.isPresent()) {
            if (existente.get().getPerfil() != PerfilUsuario.ADMIN) {
                throw new IllegalStateException("O e-mail do administrador inicial pertence a um usuário comum. Use outro e-mail.");
            }
            return; // Reiniciar não troca a senha de uma conta existente.
        }
        var admin = new Usuario("Administrador", email, senha, SetorUsuario.TI, CargoUsuario.TECNICO);
        admin.setPerfil(PerfilUsuario.ADMIN);
        service.cadastrar(admin);
    }
}
