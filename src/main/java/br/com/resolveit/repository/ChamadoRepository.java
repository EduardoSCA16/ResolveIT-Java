package br.com.resolveit.repository;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    boolean existsBySolicitante_Id(Long id);

    List<Chamado> findByStatus(StatusChamado status);
}
