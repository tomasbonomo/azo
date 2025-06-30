package com.uade.tpo.deportes.repository;

import com.uade.tpo.deportes.entity.Deporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeporteRepository extends JpaRepository<Deporte, Long> {
    
    Optional<Deporte> findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    List<Deporte> findByActivoTrue();
    
    Optional<Deporte> findByIdAndActivoTrue(Long id);
}