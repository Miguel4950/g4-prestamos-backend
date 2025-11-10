package co.edu.javeriana.prestamos.repository;

import co.edu.javeriana.prestamos.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Integer> {
    // Spring Data JPA nos da findById() gratis
}