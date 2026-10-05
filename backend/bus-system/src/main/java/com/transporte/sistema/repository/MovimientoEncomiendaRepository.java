package com.transporte.sistema.repository;
import com.transporte.sistema.entity.MovimientoEncomienda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MovimientoEncomiendaRepository extends JpaRepository<MovimientoEncomienda, Long> {
    List<MovimientoEncomienda> findByEncomiendaIdOrderByFechaHoraAsc(Long encomiendaId);
}
