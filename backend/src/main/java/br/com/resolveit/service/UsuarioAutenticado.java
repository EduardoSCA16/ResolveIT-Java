package br.com.resolveit.service;

import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("usuarioAutenticado")
public class UsuarioAutenticado {
    private final UsuarioRepository repository;

    public UsuarioAutenticado(UsuarioRepository repository) { this.repository = repository; }

    public Usuario atual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre na sua conta.");
        }
        return repository.findById(Long.valueOf(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta indisponível."));
    }

    // Consulta o banco para que uma mudança de perfil tenha efeito em sessões já abertas.
    public boolean isAdmin() { return atual().getPerfil() == PerfilUsuario.ADMIN; }
}
