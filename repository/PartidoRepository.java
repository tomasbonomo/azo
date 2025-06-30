package com.uade.tpo.deportes.repository;

import com.uade.tpo.deportes.entity.Partido;
import com.uade.tpo.deportes.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {
    
    // Búsquedas básicas
    List<Partido> findByOrganizador(Usuario organizador);
    
    @Query("SELECT p FROM Partido p WHERE :usuario MEMBER OF p.participantes")
    List<Partido> findPartidosConJugador(@Param("usuario") Usuario usuario);
    
    // Búsquedas por estado
    List<Partido> findByEstadoActual(String estado);
    
    @Query("SELECT p FROM Partido p WHERE p.estadoActual IN ('NECESITAMOS_JUGADORES', 'PARTIDO_ARMADO')")
    Page<Partido> findPartidosDisponibles(Pageable pageable);
    
    // Búsquedas por deporte
    @Query("SELECT p FROM Partido p WHERE p.deporte.nombre = :nombreDeporte")
    Page<Partido> findByNombreDeporte(@Param("nombreDeporte") String nombreDeporte, Pageable pageable);
    
    // Búsquedas por fecha
    @Query("SELECT p FROM Partido p WHERE p.horario BETWEEN :inicio AND :fin")
    List<Partido> findByHorarioBetween(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin
    );
    
    @Query("SELECT p FROM Partido p WHERE p.horario >= :fecha")
    List<Partido> findPartidosFuturos(@Param("fecha") LocalDateTime fecha);
    
    // Búsquedas complejas para la funcionalidad principal
    @Query("SELECT p FROM Partido p WHERE " +
           "p.estadoActual = 'NECESITAMOS_JUGADORES' AND " +
           "p.horario > :ahora AND " +
           "SIZE(p.participantes) < p.cantidadJugadoresRequeridos AND " +
           ":usuario NOT MEMBER OF p.participantes")
    List<Partido> findPartidosDisponiblesParaUsuario(
        @Param("usuario") Usuario usuario,
        @Param("ahora") LocalDateTime ahora
    );
    
    @Query("SELECT p FROM Partido p WHERE " +
           "p.estadoActual = 'NECESITAMOS_JUGADORES' AND " +
           "p.horario > :ahora AND " +
           "SIZE(p.participantes) < p.cantidadJugadoresRequeridos AND " +
           "p.deporte.nombre = :nombreDeporte AND " +
           ":usuario NOT MEMBER OF p.participantes")
    List<Partido> findPartidosDisponiblesPorDeporte(
        @Param("usuario") Usuario usuario,
        @Param("nombreDeporte") String nombreDeporte,
        @Param("ahora") LocalDateTime ahora
    );
    
    // Búsquedas por zona geográfica
    @Query("SELECT p FROM Partido p WHERE " +
           "p.estadoActual = 'NECESITAMOS_JUGADORES' AND " +
           "p.ubicacion.zona = :zona AND " +
           "p.horario > :ahora AND " +
           ":usuario NOT MEMBER OF p.participantes")
    List<Partido> findPartidosDisponiblesPorZona(
        @Param("usuario") Usuario usuario,
        @Param("zona") String zona,
        @Param("ahora") LocalDateTime ahora
    );
    
    // Consultas para notificaciones
    @Query("SELECT p FROM Partido p WHERE " +
           "p.estadoActual = 'CONFIRMADO' AND " +
           "p.horario <= :momento AND " +
           "p.horario > :hace5Minutos")
    List<Partido> findPartidosParaIniciar(
        @Param("momento") LocalDateTime momento,
        @Param("hace5Minutos") LocalDateTime hace5Minutos
    );
    
    @Query("SELECT p FROM Partido p WHERE " +
           "p.estadoActual = 'EN_JUEGO' AND " +
           "p.horario <= :momentoFinalizacion")
    List<Partido> findPartidosParaFinalizar(@Param("momentoFinalizacion") LocalDateTime momentoFinalizacion);
    
    // Estadísticas
    @Query("SELECT COUNT(p) FROM Partido p WHERE p.estadoActual = :estado")
    long countByEstado(@Param("estado") String estado);
    
    @Query("SELECT p.deporte.nombre, COUNT(p) FROM Partido p GROUP BY p.deporte.nombre")
    List<Object[]> contarPartidosPorDeporte();
    
    @Query("SELECT p.estadoActual, COUNT(p) FROM Partido p GROUP BY p.estadoActual")
    List<Object[]> contarPartidosPorEstado();
    
    @Query("SELECT AVG(SIZE(p.participantes)) FROM Partido p WHERE p.estadoActual = 'FINALIZADO'")
    Double promedioJugadoresPorPartido();
    
    // Búsquedas para el historial de un usuario
    @Query("SELECT p FROM Partido p WHERE " +
           "(p.organizador = :usuario OR :usuario MEMBER OF p.participantes) AND " +
           "p.estadoActual = :estado " +
           "ORDER BY p.horario DESC")
    List<Partido> findHistorialUsuario(
        @Param("usuario") Usuario usuario,
        @Param("estado") String estado
    );

    @Query("SELECT p FROM Partido p WHERE p.ubicacion.zona = :zona AND p.horario > :ahora")
    List<Partido> findTodosPorZona(
        @Param("zona") String zona,
        @Param("ahora") LocalDateTime ahora
    );
}
