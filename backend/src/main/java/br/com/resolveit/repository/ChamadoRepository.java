package br.com.resolveit.repository;

import br.com.resolveit.enums.StatusChamado;
import br.com.resolveit.model.Chamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    boolean existsBySolicitante_Id(Long id);

    Page<Chamado> findByStatus(StatusChamado status, Pageable pageable);

    Page<Chamado> findBySolicitanteId(Long id, Pageable pageable);

    Page<Chamado> findByStatusAndSolicitanteId(StatusChamado status, Long id, Pageable pageable);
}
