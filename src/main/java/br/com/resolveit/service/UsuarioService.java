package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarUsuarioRequest;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.repository.ChamadoRepository;
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
    private final ChamadoRepository chamadoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          ChamadoRepository chamadoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.chamadoRepository = chamadoRepository;
    }

    public void cadastrar(Usuario usuario) {
        boolean emailCadastrado = usuarioRepository.existsByEmail(usuario.getEmail());
        if(emailCadastrado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email já cadastrado!"
            );
        }
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

    public Usuario atualizar(Long id, AtualizarUsuarioRequest dados) {
        Usuario usuarioExistente = buscarPorId(id);
        if(usuarioRepository.existsByEmailAndIdNot(dados.email(), id)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este email já está sendo usado!"
            );
        }
        usuarioExistente.setNome(dados.nome());
        usuarioExistente.setEmail(dados.email());
        usuarioExistente.setSetor(dados.setor());

        return usuarioRepository.save(usuarioExistente);
    }

    public void deletar(Long id) {
        Usuario usuario = buscarPorId(id);
        if(chamadoRepository.existsBySolicitante_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível excluir um usuário que possui chamados!"
            );
        }
        usuarioRepository.delete(usuario);
    }
}
