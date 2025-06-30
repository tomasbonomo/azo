package com.uade.tpo.deportes.entity;

import com.uade.tpo.deportes.patterns.state.EstadoPartido;
import com.uade.tpo.deportes.patterns.strategy.EstrategiaEmparejamiento;
import com.uade.tpo.deportes.patterns.observer.ObservablePartido;
import com.uade.tpo.deportes.patterns.observer.ObserverPartido;
import com.uade.tpo.deportes.patterns.state.NecesitamosJugadoresState;
import com.uade.tpo.deportes.patterns.state.PartidoArmadoState;
import com.uade.tpo.deportes.patterns.state.ConfirmadoState;
import com.uade.tpo.deportes.patterns.state.EnJuegoState;
import com.uade.tpo.deportes.patterns.state.FinalizadoState;
import com.uade.tpo.deportes.patterns.state.CanceladoState;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "partidos")
public class Partido implements ObservablePartido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deporte_id", nullable = false)
    private Deporte deporte;

    @Column(nullable = false)
    private Integer cantidadJugadoresRequeridos;

    @Column(nullable = false)
    private Integer duracion; // en minutos

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(nullable = false)
    private LocalDateTime horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @ManyToMany
    @JoinTable(
        name = "partido_participantes",
        joinColumns = @JoinColumn(name = "partido_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @Builder.Default
    private List<Usuario> participantes = new ArrayList<>();

    @Column(nullable = false)
    private String estadoActual = "NECESITAMOS_JUGADORES";

    @Column(nullable = false)
    private String estrategiaActual = "POR_NIVEL";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Campos transient para los patrones
    @Transient
    private EstadoPartido estado;

    @Transient
    private EstrategiaEmparejamiento estrategiaEmparejamiento;

    @Transient
    @Builder.Default
    private List<ObserverPartido> observers = new ArrayList<>();

    // Métodos de negocio
    public void agregarJugador(Usuario usuario) {
        if (puedeUnirse(usuario)) {
            participantes.add(usuario);
            notificarObservers();
        }
    }

    public boolean puedeUnirse(Usuario usuario) {
        return estrategiaEmparejamiento != null && 
               estrategiaEmparejamiento.puedeUnirse(usuario, this);
    }

    public List<Usuario> getParticipantes() {
        return participantes;
    }

    public void cambiarEstado(String nuevoEstado) {
        // No se puede cancelar ni cambiar desde EN_JUEGO, FINALIZADO o CANCELADO
        if ("EN_JUEGO".equals(this.estadoActual) || "FINALIZADO".equals(this.estadoActual) || "CANCELADO".equals(this.estadoActual)) {
            return;
        }
        EstadoPartido nuevo = obtenerEstadoPorNombre(nuevoEstado);
        // Permitir cancelar desde cualquier estado intermedio
        if ("CANCELADO".equals(nuevoEstado)) {
            this.estado = nuevo;
            this.estadoActual = nuevoEstado;
            notificarObservers();
            return;
        }
        // Solo permite avanzar al siguiente estado lógico
        if (this.estado == null) {
            this.estado = obtenerEstadoPorNombre(this.estadoActual);
        }
        EstadoPartido siguiente = this.estado.obtenerEstadoSiguiente();
        if (siguiente.getNombre().equals(nuevoEstado)) {
            this.estado = nuevo;
            this.estadoActual = nuevoEstado;
            notificarObservers();
        }
        // Si intenta una transición inválida, ignora el cambio
    }

    private EstadoPartido obtenerEstadoPorNombre(String nombre) {
        switch (nombre) {
            case "NECESITAMOS_JUGADORES":
                return new NecesitamosJugadoresState();
            case "PARTIDO_ARMADO":
                return new PartidoArmadoState();
            case "CONFIRMADO":
                return new ConfirmadoState();
            case "EN_JUEGO":
                return new EnJuegoState();
            case "FINALIZADO":
                return new FinalizadoState();
            case "CANCELADO":
                return new CanceladoState();
            default:
                return new NecesitamosJugadoresState();
        }
    }

    // Implementación ObservablePartido
    @Override
    public void agregarObserver(ObserverPartido observer) {
        System.out.println("➕ Agregando observer: " + observer.getClass().getSimpleName());
        if (observers == null) {
            observers = new ArrayList<>();
            System.out.println("   ✅ Lista de observers inicializada");
        }
        observers.add(observer);
        System.out.println("   ✅ Observer agregado. Total de observers: " + observers.size());
    }

    @Override
    public void removerObserver(ObserverPartido observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notificarObservers() {
        System.out.println("🔔 === NOTIFICANDO OBSERVERS ===");
        System.out.println("   Partido ID: " + this.id);
        System.out.println("   Estado actual: " + this.estadoActual);
        System.out.println("   Observers registrados: " + (observers != null ? observers.size() : 0));
        
        if (observers != null) {
            List<ObserverPartido> copiaObservers = new ArrayList<>(observers);
            int idx = 0;
            for (ObserverPartido observer : copiaObservers) {
                try {
                    System.out.println("   Ejecutando observer #" + idx + " - Tipo: " + observer.getClass().getSimpleName());
                    observer.actualizar(this, determinarEvento());
                    System.out.println("   ✅ Observer #" + idx + " ejecutado exitosamente");
                } catch (Exception e) {
                    System.err.println("   ❌ Error en observer #" + idx + ": " + e.getMessage());
                    e.printStackTrace();
                }
                idx++;
            }
        } else {
            System.err.println("   ❌ No hay observers registrados");
        }
    }

    public void notificarObservers(com.uade.tpo.deportes.enums.EventoPartido evento) {
        for (ObserverPartido observer : observers) {
            observer.actualizar(this, evento);
        }
    }

    private com.uade.tpo.deportes.enums.EventoPartido determinarEvento() {
        System.out.println("🔍 Determinando evento para estado: " + estadoActual);
        // Lógica para determinar qué evento disparar basado en el estado
        com.uade.tpo.deportes.enums.EventoPartido evento;
        switch (estadoActual) {
            case "NECESITAMOS_JUGADORES":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_CREADO;
                break;
            case "PARTIDO_ARMADO":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_ARMADO;
                break;
            case "CONFIRMADO":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_CONFIRMADO;
                break;
            case "EN_JUEGO":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_INICIADO;
                break;
            case "FINALIZADO":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_FINALIZADO;
                break;
            case "CANCELADO":
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_CANCELADO;
                break;
            default:
                evento = com.uade.tpo.deportes.enums.EventoPartido.PARTIDO_CREADO;
                break;
        }
        System.out.println("🔍 Evento determinado: " + evento);
        return evento;
    }
}