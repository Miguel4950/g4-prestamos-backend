package co.edu.javeriana.prestamos.repository;

import co.edu.javeriana.prestamos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // G2 ya definió esto, lo necesitamos para validar el usuario
    Optional<Usuario> findByUsername(String username);
}