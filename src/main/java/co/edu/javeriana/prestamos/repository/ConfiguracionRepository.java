package co.edu.javeriana.prestamos.repository;

import co.edu.javeriana.prestamos.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {
    Optional<Configuracion> findByClave(String clave);
}
