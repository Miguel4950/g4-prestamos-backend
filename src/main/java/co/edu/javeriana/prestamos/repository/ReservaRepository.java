package co.edu.javeriana.prestamos.repository;

import co.edu.javeriana.prestamos.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r " +
           "WHERE r.id_usuario = :usuarioId AND r.id_libro = :libroId AND r.id_estado_reserva IN :estados")
    boolean existsActiveOrNotified(@Param("usuarioId") Integer usuarioId,
                                   @Param("libroId") Integer libroId,
                                   @Param("estados") List<Integer> estados);

    @Query("SELECT r FROM Reserva r WHERE r.id_usuario = :usuarioId ORDER BY r.fecha_reserva DESC")
    List<Reserva> findByUsuario(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT r FROM Reserva r WHERE r.id_libro = :libroId AND r.id_estado_reserva = :estado ORDER BY r.fecha_reserva ASC")
    List<Reserva> findQueueByLibroAndEstado(@Param("libroId") Integer libroId,
                                            @Param("estado") Integer estado);

    @Query("SELECT r FROM Reserva r WHERE (:estado IS NULL OR r.id_estado_reserva = :estado) ORDER BY r.fecha_reserva DESC")
    List<Reserva> findAllByEstadoOptional(@Param("estado") Integer estado);

    @Query("SELECT r FROM Reserva r WHERE r.id_estado_reserva = :estado AND r.fecha_reserva <= :before")
    List<Reserva> findOlderThanWithEstado(@Param("estado") Integer estado,
                                          @Param("before") LocalDateTime before);
}

