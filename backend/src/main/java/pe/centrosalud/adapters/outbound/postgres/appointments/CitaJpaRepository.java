package pe.centrosalud.adapters.outbound.postgres.appointments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.centrosalud.modules.appointments.domain.EstadoCita;

interface CitaJpaRepository extends JpaRepository<CitaJpaEntity, UUID> {
    interface EstadoConteo {
        EstadoCita getEstado();
        Long getCantidad();
    }

    @Query("""
            select c from CitaJpaEntity c
            where c.inicio >= :desde and c.inicio < :hasta
              and (:profesionalId is null or c.profesionalId = :profesionalId)
              and (:servicioId is null or c.servicioId = :servicioId)
              and (:consultorioId is null or c.consultorioId = :consultorioId)
            order by c.inicio asc, c.llegadaEn asc, c.id asc
            """)
    List<CitaJpaEntity> agenda(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                               @Param("profesionalId") UUID profesionalId, @Param("servicioId") UUID servicioId,
                               @Param("consultorioId") UUID consultorioId);

    @Query("""
            select c from CitaJpaEntity c
            where c.inicio >= :desde and c.inicio < :hasta
              and c.estado in :estados
              and (:profesionalId is null or c.profesionalId = :profesionalId)
            order by c.inicio asc, c.llegadaEn asc, c.id asc
            """)
    List<CitaJpaEntity> atencion(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                 @Param("profesionalId") UUID profesionalId,
                                 @Param("estados") List<EstadoCita> estados);

    @Query("""
            select c.estado as estado, count(c) as cantidad from CitaJpaEntity c
            where c.inicio >= :desde and c.inicio < :hasta
              and (:profesionalId is null or c.profesionalId = :profesionalId)
              and (:servicioId is null or c.servicioId = :servicioId)
              and (:consultorioId is null or c.consultorioId = :consultorioId)
            group by c.estado
            """)
    List<EstadoConteo> conteo(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                              @Param("profesionalId") UUID profesionalId, @Param("servicioId") UUID servicioId,
                              @Param("consultorioId") UUID consultorioId);

    @Query("""
            select count(c) > 0 from CitaJpaEntity c
            where c.estado not in (:cerrados)
              and c.inicio < :fin and c.fin > :inicio
              and (c.pacienteId = :pacienteId or c.profesionalId = :profesionalId or c.consultorioId = :consultorioId)
              and (:excluirId is null or c.id <> :excluirId)
            """)
    boolean existeConflicto(@Param("pacienteId") UUID pacienteId, @Param("profesionalId") UUID profesionalId,
                            @Param("consultorioId") UUID consultorioId, @Param("inicio") Instant inicio,
                            @Param("fin") Instant fin, @Param("excluirId") UUID excluirId,
                            @Param("cerrados") List<EstadoCita> cerrados);
}
