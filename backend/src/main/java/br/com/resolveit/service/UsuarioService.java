package br.com.resolveit.service;

import br.com.resolveit.dto.AtualizarUsuarioRequest;
import br.com.resolveit.model.Usuario;
import br.com.resolveit.enums.PerfilUsuario;
import br.com.resolveit.repository.ChamadoRepository;
import br.com.resolveit.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioAutenticado autenticado;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          ChamadoRepository chamadoRepository, UsuarioAutenticado autenticado) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.chamadoRepository = chamadoRepository;
        this.autenticado = autenticado;
    }

    @Transactional
    public void cadastrar(Usuario usuario) {
        usuario.setNome(usuario.getNome().trim());
        usuario.setEmail(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        if (usuario.getSenha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha excede 72 bytes. Use menos caracteres especiais.");
        }
        boolean emailCadastrado = usuarioRepository.existsByEmailIgnoreCase(usuario.getEmail());
        if(emailCadastrado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email já cadastrado!"
            );
        }
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioRepository.save(usuario);
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    @Transactional
    public Usuario atualizar(Long id, AtualizarUsuarioRequest dados) {
        Usuario usuarioExistente = buscarPorId(id);
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        if (id.equals(autenticado.atual().getId()) && dados.perfil() != PerfilUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode remover seu próprio acesso de administrador.");
        }
        if(usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este email já está sendo usado!"
            );
        }
        usuarioExistente.setNome(dados.nome().trim());
        usuarioExistente.setEmail(email);
        usuarioExistente.setSetor(dados.setor());
        usuarioExistente.setPerfil(dados.perfil());

        return usuarioRepository.save(usuarioExistente);
    }

    @PreAuthorize("@usuarioAutenticado.isAdmin()")
    @Transactional
    public void deletar(Long id) {
        if (id.equals(autenticado.atual().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode excluir sua própria conta de administrador.");
        }
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
