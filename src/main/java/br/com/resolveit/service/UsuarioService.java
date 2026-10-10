package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarUsuarioRequest;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void cadastrar(Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));
    }

    public Usuario atualizarUsuario(Long id, AtualizarUsuarioRequest dados) {
        Usuario usuarioExistente = buscarPorId(id);
        usuarioExistente.setNome(dados.nome());
        usuarioExistente.setEmail(dados.email());
        usuarioExistente.setSetor(dados.setor());

        return usuarioRepository.save(usuarioExistente);
    }
}
