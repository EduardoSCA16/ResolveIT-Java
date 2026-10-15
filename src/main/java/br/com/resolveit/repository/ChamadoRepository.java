package br.com.resolveit.repository;

import br.com.resolveit.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    boolean existsBySolicitante_Id(Long id);
}
