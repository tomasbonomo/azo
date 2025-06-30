package com.uade.tpo.deportes.service.partido;

import com.uade.tpo.deportes.dto.*;
import com.uade.tpo.deportes.entity.Deporte;
import com.uade.tpo.deportes.entity.Partido;
import com.uade.tpo.deportes.entity.Ubicacion;
import com.uade.tpo.deportes.entity.Usuario;
import com.uade.tpo.deportes.exceptions.PartidoNoEncontradoException;
import com.uade.tpo.deportes.exceptions.UsuarioNoAutorizadoException;
import com.uade.tpo.deportes.patterns.observer.NotificadorObserver;
import com.uade.tpo.deportes.patterns.state.*;
import com.uade.tpo.deportes.patterns.strategy.*;
import com.uade.tpo.deportes.repository.DeporteRepository;
import com.uade.tpo.deportes.repository.PartidoRepository;
import com.uade.tpo.deportes.repository.UbicacionRepository;
import com.uade.tpo.deportes.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.deportes.service.comentarios.ComentarioService;
import com.uade.tpo.deportes.service.confirmacion.ConfirmacionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.uade.tpo.deportes.service.partido.NotificacionAsyncService;
import com.uade.tpo.deportes.enums.NivelJuego;
import com.uade.tpo.deportes.enums.EventoPartido;

@Service
@RequiredArgsConstructor
public class PartidoServiceImpl implements PartidoService {
    @Autowired
    private ComentarioService comentarioService;
    @Autowired
    private ConfirmacionService confirmacionService;

    @Autowired
    private PartidoRepository partidoRepository;
    
    @Autowired
    private DeporteRepository deporteRepository;
    
    @Autowired
    private UbicacionRepository ubicacionRepository;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private NotificadorObserver notificadorObserver;
    
    @Autowired
    private EmparejamientoPorNivelStrategy emparejamientoPorNivel;
    
    @Autowired
    private EmparejamientoPorCercaniaStrategy emparejamientoPorCercania;
    
    @Autowired
    private EmparejamientoPorHistorialStrategy emparejamientoPorHistorial;
     @Autowired
    private NotificacionAsyncService notificacionAsyncService;
    @Override
    @Transactional
    public PartidoResponse crearPartido(String emailOrganizador, CrearPartidoRequest request) {
        // Validaciones robustas y mensajes claros
        if (request.getDeporteId() == null) {
            throw new IllegalArgumentException("El id del deporte es obligatorio");
        }
        if (request.getCantidadJugadoresRequeridos() < 2) {
            throw new IllegalArgumentException("La cantidad mínima de jugadores es 2");
        }
        if (request.getDuracion() <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0 minutos");
        }
        if (request.getUbicacion() == null || request.getUbicacion().getDireccion() == null || request.getUbicacion().getDireccion().trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        if (request.getHorario() == null || request.getHorario().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("El horario debe ser igual o posterior al momento de creación");
        }
        // Obtener organizador
        Usuario organizador = usuarioService.obtenerUsuarioPorEmail(emailOrganizador);
        
        // Crear o obtener deporte
        Deporte deporte = deporteRepository.findById(request.getDeporteId())
                .orElseThrow(() -> new RuntimeException("Deporte no encontrado"));
        
        // Crear ubicación
        Ubicacion ubicacion = Ubicacion.builder()
                .direccion(request.getUbicacion().getDireccion())
                .latitud(request.getUbicacion().getLatitud())
                .longitud(request.getUbicacion().getLongitud())
                .zona(request.getUbicacion().getZona())
                .build();
        ubicacionRepository.save(ubicacion);
        
        // Crear partido
        Partido partido = Partido.builder()
                .deporte(deporte)
                .cantidadJugadoresRequeridos(request.getCantidadJugadoresRequeridos())
                .duracion(request.getDuracion())
                .ubicacion(ubicacion)
                .horario(request.getHorario())
                .organizador(organizador)
                .participantes(new ArrayList<>())
                .estadoActual("NECESITAMOS_JUGADORES")
                .estrategiaActual(request.getEstrategiaEmparejamiento() != null ? 
                    request.getEstrategiaEmparejamiento() : "POR_NIVEL")
                .build();
        // Configurar observers ANTES de guardar
      if (partido.getObservers() == null) {
        partido.setObservers(new ArrayList<>());
        }
        
        // Configurar estrategia
        configurarEstrategiaInterna(partido, request.getEstrategiaEmparejamiento());
        
        // Guardar partido
        partidoRepository.save(partido);
        
        System.out.println("💾 Partido guardado con ID: " + partido.getId());
        
        // ✅ FIXED: Reconectar observers después de guardar (se pierden al persistir)
        System.out.println("🔌 Reconectando observers después de guardar...");
        reconectarObservers(partido);
        
        // Dejar solo la notificación asíncrona
        System.out.println("🔔 Llamando a notificacionAsyncService.notificarCreacionPartido()...");
        notificacionAsyncService.notificarCreacionPartido(partido);
        System.out.println("✅ notificacionAsyncService.notificarCreacionPartido() llamado");
        
        // Responder al usuario inmediatamente
        return mapearAResponse(partido, organizador);
    }

    @Override
    public PartidoResponse obtenerPartido(Long partidoId) {
        Partido partido = obtenerPartidoPorId(partidoId);
        return mapearAResponse(partido, null);
    }

    @Override
    public List<PartidoResponse> obtenerPartidosDelUsuario(String email) {
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email);
        
        List<Partido> partidosOrganizados = partidoRepository.findByOrganizador(usuario);
        List<Partido> partidosJugados = partidoRepository.findPartidosConJugador(usuario);
        
        // Combinar y eliminar duplicados
        List<Partido> todosLosPartidos = new ArrayList<>(partidosOrganizados);
        partidosJugados.forEach(p -> {
            if (!todosLosPartidos.contains(p)) {
                todosLosPartidos.add(p);
            }
        });
        
        return todosLosPartidos.stream()
                .map(p -> mapearAResponse(p, usuario))
                .collect(Collectors.toList());
    }

@Override
public Page<PartidoResponse> buscarPartidos(String emailUsuario, CriteriosBusqueda criterios, Pageable pageable) {
    Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
    
    System.out.println("🔍 === BÚSQUEDA INTELIGENTE INICIADA ===");
    System.out.println("👤 Usuario: " + usuario.getNombreUsuario() + " (Nivel: " + usuario.getNivelJuego() + ")");
    
    // ⚡ PASO 1: Obtener partidos candidatos
    List<Partido> partidos = obtenerPartidosCandidatos(usuario, criterios);
    System.out.println("📊 Partidos candidatos encontrados: " + partidos.size());
    
    // ⚡ PASO 2: Aplicar filtros
    partidos = aplicarFiltrosInteligentes(partidos, criterios);
    System.out.println("🔧 Partidos después de filtros: " + partidos.size());
    
    // ⚡ PASO 3: Configurar estrategias y calcular compatibilidad
    partidos.forEach(p -> {
        configurarEstrategiaInterna(p, p.getEstrategiaActual());
    });
    
    // ⚡ PASO 4: Ordenar por compatibilidad
    partidos = ordenarPartidosPorCompatibilidad(partidos, criterios, usuario);
    
    // ⚡ PASO 5: Convertir a responses
    List<PartidoResponse> responses = partidos.stream()
            .map(p -> mapearAResponseConCompatibilidad(p, usuario))
            .collect(Collectors.toList());
    
    // ⚡ PASO 6: Paginación
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), responses.size());
    List<PartidoResponse> pageContent = responses.subList(start, end);
    
    System.out.println("📄 Página devuelta: " + pageContent.size() + " partidos");
    System.out.println("🔍 === BÚSQUEDA COMPLETADA ===\n");
    
    return new PageImpl<>(pageContent, pageable, responses.size());
}

// 🎯 MÉTODOS AUXILIARES SIMPLIFICADOS

private List<Partido> obtenerPartidosCandidatos(Usuario usuario, CriteriosBusqueda criterios) {
    LocalDateTime ahora = LocalDateTime.now();
    if (criterios.getDeporteId() != null) {
        Deporte deporte = deporteRepository.findById(criterios.getDeporteId())
            .orElseThrow(() -> new RuntimeException("Deporte no encontrado"));
        System.out.println("🎾 Búsqueda por deporte: " + deporte.getNombre());
        return partidoRepository.findPartidosDisponiblesPorDeporte(usuario, deporte.getNombre(), ahora);
    } else if (criterios.getZona() != null) {
        System.out.println("🗺️ Búsqueda por zona: " + criterios.getZona());
        if (criterios.getIncluirTodos() != null && criterios.getIncluirTodos()) {
            // Nuevo: traer todos los partidos de la zona, sin filtrar por participación
            return partidoRepository.findTodosPorZona(criterios.getZona(), ahora);
        } else {
            return partidoRepository.findPartidosDisponiblesPorZona(usuario, criterios.getZona(), ahora);
        }
    } else if (criterios.getIncluirTodos() != null && criterios.getIncluirTodos()) {
        // Nuevo: si incluirTodos es true y no hay zona ni deporte, traer todos los partidos futuros
        return partidoRepository.findPartidosFuturos(ahora);
    } else {
        System.out.println("🌟 Búsqueda general");
        return partidoRepository.findPartidosDisponiblesParaUsuario(usuario, ahora);
    }
}

private List<Partido> aplicarFiltrosInteligentes(List<Partido> partidos, CriteriosBusqueda criterios) {
    return partidos.stream()
        // Solo partidos disponibles
        .filter(p -> p.getParticipantes().size() < p.getCantidadJugadoresRequeridos())
        .collect(Collectors.toList());
}

private List<Partido> ordenarPartidosPorCompatibilidad(List<Partido> partidos, CriteriosBusqueda criterios, Usuario usuario) {
    return partidos.stream()
            .sorted((p1, p2) -> {
                double compat1 = p1.getEstrategiaEmparejamiento() != null ? 
                    p1.getEstrategiaEmparejamiento().calcularCompatibilidad(usuario, p1) : 0.0;
                double compat2 = p2.getEstrategiaEmparejamiento() != null ? 
                    p2.getEstrategiaEmparejamiento().calcularCompatibilidad(usuario, p2) : 0.0;
                
                // 🎯 BONUS POR DEPORTE FAVORITO
                if (usuario.getDeporteFavorito() != null && 
                    usuario.getDeporteFavorito().equals(p1.getDeporte().getNombre())) {
                    compat1 += 0.1;
                }
                if (usuario.getDeporteFavorito() != null && 
                    usuario.getDeporteFavorito().equals(p2.getDeporte().getNombre())) {
                    compat2 += 0.1;
                }
                
                return Double.compare(compat2, compat1); // Mayor compatibilidad primero
            })
            .collect(Collectors.toList());
}

private boolean esHorarioConveniente(LocalDateTime horario) {
    int hora = horario.getHour();
    int diaSemana = horario.getDayOfWeek().getValue();
    
    if (diaSemana <= 5) { // Lunes a Viernes
        return hora >= 17 && hora <= 21;
    } else { // Fin de semana
        return hora >= 10 && hora <= 22;
    }
}

private PartidoResponse mapearAResponseConCompatibilidad(Partido partido, Usuario usuario) {
    boolean puedeUnirse = partido.puedeUnirse(usuario);
    
    // Calcular compatibilidad final
    double compatibilidad = 0.0;
    if (partido.getEstrategiaEmparejamiento() != null) {
        compatibilidad = partido.getEstrategiaEmparejamiento().calcularCompatibilidad(usuario, partido);
    }
    
    // Aplicar mismos bonus que en ordenamiento
    if (usuario.getDeporteFavorito() != null && 
        usuario.getDeporteFavorito().equals(partido.getDeporte().getNombre())) {
        compatibilidad += 0.1;
    }
    
    if (esHorarioConveniente(partido.getHorario())) {
        compatibilidad += 0.05;
    }
    
    compatibilidad = Math.max(0.0, Math.min(1.0, compatibilidad));
    
    return PartidoResponse.builder()
            .id(partido.getId())
            .deporte(mapearDeporteAResponse(partido.getDeporte()))
            .cantidadJugadoresRequeridos(partido.getCantidadJugadoresRequeridos())
            .cantidadJugadoresActual(partido.getParticipantes().size())
            .duracion(partido.getDuracion())
            .ubicacion(mapearUbicacionAResponse(partido.getUbicacion()))
            .horario(partido.getHorario())
            .organizador(mapearUsuarioAResponse(partido.getOrganizador()))
            .jugadores(partido.getParticipantes().stream()
                    .map(this::mapearUsuarioAResponse)
                    .collect(Collectors.toList()))
            .estado(partido.getEstadoActual())
            .estrategiaEmparejamiento(partido.getEstrategiaActual())
            .createdAt(partido.getCreatedAt())
            .puedeUnirse(puedeUnirse)
            .compatibilidad(compatibilidad) // ⭐ COMPATIBILIDAD CALCULADA
            .build();
}

    @Override
@Transactional
public MessageResponse unirseAPartido(String emailUsuario, Long partidoId) {
    Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
    Partido partido = obtenerPartidoPorId(partidoId);
    
    // ✅ RECONECTAR OBSERVERS (por si se perdieron al cargar de BD)
    reconectarObservers(partido);
    
    // Configurar estrategia
    configurarEstrategiaInterna(partido, partido.getEstrategiaActual());
    
    // Configurar estado
    EstadoPartido estado = obtenerEstadoPorNombre(partido.getEstadoActual());
    
    try {
        int jugadoresAntes = partido.getParticipantes().size();
        
        // Intentar unirse
        estado.manejarSolicitudUnion(partido, usuario);
        
        // ✅ VERIFICAR SI AHORA ESTÁ COMPLETO (REQUERIMIENTO TPO)
        if (jugadoresAntes < partido.getCantidadJugadoresRequeridos() && 
            partido.getParticipantes().size() >= partido.getCantidadJugadoresRequeridos()) {
            
            // Cambiar estado automáticamente
            partido.cambiarEstado("PARTIDO_ARMADO");
            System.out.println("🎯 Partido " + partidoId + " ahora está ARMADO - Disparando notificaciones");
        }
        
        // Guardar cambios
        partidoRepository.save(partido);
        
        // ✅ NOTIFICAR que un jugador se unió
        partido.notificarObservers(EventoPartido.JUGADOR_UNIDO);
        
        return MessageResponse.success("Te has unido al partido exitosamente");
        
    } catch (IllegalArgumentException | IllegalStateException e) {
        return MessageResponse.error("No puedes unirte al partido", e.getMessage());
    }
}

    @Override
    @Transactional  
    public MessageResponse cambiarEstadoPartido(String emailOrganizador, Long partidoId, CambiarEstadoPartidoRequest request) {
        Usuario organizador = usuarioService.obtenerUsuarioPorEmail(emailOrganizador);
        Partido partido = obtenerPartidoPorId(partidoId);
        
        // Permitir que el admin o el organizador cambien el estado
        if (!partido.getOrganizador().equals(organizador) && !organizador.getRole().name().equals("ADMIN")) {
            throw new UsuarioNoAutorizadoException("Solo el organizador o un admin pueden cambiar el estado del partido");
        }
        
        // ✅ RECONECTAR OBSERVERS
        reconectarObservers(partido);
        
        String estadoAnterior = partido.getEstadoActual();
        
        // Cambiar estado
        partido.cambiarEstado(request.getNuevoEstado());
        partidoRepository.save(partido);
        
        // ✅ NOTIFICAR CAMBIO DE ESTADO SOLO UNA VEZ
        System.out.println("🔔 Estado cambió de " + estadoAnterior + " → " + request.getNuevoEstado() + 
                          " - Disparando notificaciones");
        notificacionAsyncService.notificarCreacionPartido(partido);
    
        // Acciones adicionales por estado
        if ("PARTIDO_ARMADO".equals(request.getNuevoEstado())) {
            confirmacionService.crearConfirmacionesPendientes(partido);
        }
        if ("FINALIZADO".equals(request.getNuevoEstado())) {
            comentarioService.generarEstadisticasAlFinalizar(partido);
        }
        
        return MessageResponse.success("Estado del partido actualizado a: " + request.getNuevoEstado());
    }
/**
 * 🔌 RECONECTAR OBSERVERS - Soluciona problema de pérdida de observers al cargar de BD
 */
private void reconectarObservers(Partido partido) {
    try {
        System.out.println("🔌 === RECONECTANDO OBSERVERS ===");
        System.out.println("   Partido ID: " + partido.getId());
        System.out.println("   Observers antes: " + (partido.getObservers() != null ? partido.getObservers().size() : 0));
        
        // Limpiar observers existentes
        if (partido.getObservers() != null) {
            partido.getObservers().clear();
            System.out.println("   ✅ Observers existentes limpiados");
        } else {
            partido.setObservers(new ArrayList<>());
            System.out.println("   ✅ Nueva lista de observers creada");
        }
        
        // Reagregar todos los observers necesarios
        System.out.println("   ➕ Agregando NotificadorObserver...");
        partido.agregarObserver(notificadorObserver);
        
        System.out.println("   ✅ Observers reconectados para partido " + partido.getId());
        System.out.println("   Total observers: " + partido.getObservers().size());
        
    } catch (Exception e) {
        System.err.println("❌ Error reconectando observers: " + e.getMessage());
        e.printStackTrace();
    }
}
    @Override
    @Transactional
    public MessageResponse configurarEstrategia(Long partidoId, ConfigurarEstrategiaRequest request) {
        Partido partido = obtenerPartidoPorId(partidoId);
        
        partido.setEstrategiaActual(request.getTipoEstrategia());
        configurarEstrategiaInterna(partido, request.getTipoEstrategia());
        
        // Configurar parámetros específicos
        if ("POR_NIVEL".equals(request.getTipoEstrategia()) && partido.getEstrategiaEmparejamiento() instanceof EmparejamientoPorNivelStrategy) {
            EmparejamientoPorNivelStrategy estrategia = (EmparejamientoPorNivelStrategy) partido.getEstrategiaEmparejamiento();
            if (request.getNivelMinimo() != null) {
                estrategia.setNivelMinimo(request.getNivelMinimo());
            }
            if (request.getNivelMaximo() != null) {
                estrategia.setNivelMaximo(request.getNivelMaximo());
            }
        }
        
        partidoRepository.save(partido);
        
        return MessageResponse.success("Estrategia de emparejamiento configurada");
    }

    @Override
    public Partido obtenerPartidoPorId(Long id) {
        return partidoRepository.findById(id)
                .orElseThrow(() -> new PartidoNoEncontradoException("Partido no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void procesarTransicionesAutomaticas() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // Partidos para iniciar
        List<Partido> partidosParaIniciar = partidoRepository.findPartidosParaIniciar(
            ahora, ahora.minusMinutes(5));
        
        partidosParaIniciar.forEach(p -> {
            p.cambiarEstado("EN_JUEGO");
            partidoRepository.save(p);
        });
        
        // Partidos para finalizar
        List<Partido> partidosParaFinalizar = partidoRepository.findPartidosParaFinalizar(
            ahora.minusMinutes(90)); // Asumiendo duración promedio
        
        partidosParaFinalizar.forEach(p -> {
            p.cambiarEstado("FINALIZADO");
            partidoRepository.save(p);
        });
    }

    // Métodos auxiliares privados
    private void configurarEstrategiaInterna(Partido partido, String tipoEstrategia) {
        System.out.println("⚙️ === CONFIGURANDO ESTRATEGIA INTERNA ===");
        System.out.println("   Partido ID: " + partido.getId());
        System.out.println("   Tipo estrategia solicitada: " + tipoEstrategia);
        System.out.println("   Estrategia actual antes: " + partido.getEstrategiaActual());
        
        switch (tipoEstrategia) {
            case "POR_NIVEL":
                System.out.println("   ➕ Configurando estrategia POR_NIVEL");
                partido.setEstrategiaEmparejamiento(emparejamientoPorNivel);
                break;
            case "POR_CERCANIA":
                System.out.println("   ➕ Configurando estrategia POR_CERCANIA");
                partido.setEstrategiaEmparejamiento(emparejamientoPorCercania);
                break;
            case "POR_HISTORIAL":
                System.out.println("   ➕ Configurando estrategia POR_HISTORIAL");
                partido.setEstrategiaEmparejamiento(emparejamientoPorHistorial);
                break;
            default:
                System.out.println("   ⚠️ Estrategia no reconocida, usando POR_NIVEL por defecto");
                partido.setEstrategiaEmparejamiento(emparejamientoPorNivel);
        }
        
        System.out.println("   ✅ Estrategia configurada: " + 
            (partido.getEstrategiaEmparejamiento() != null ? 
             partido.getEstrategiaEmparejamiento().getClass().getSimpleName() : "NULL"));
        System.out.println("   ✅ Nombre de la estrategia: " + 
            (partido.getEstrategiaEmparejamiento() != null ? 
             partido.getEstrategiaEmparejamiento().getNombre() : "NULL"));
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

    private PartidoResponse mapearAResponse(Partido partido, Usuario usuario) {
        boolean puedeUnirse = usuario != null && partido.puedeUnirse(usuario);
        Double compatibilidad = usuario != null && partido.getEstrategiaEmparejamiento() != null ? 
                partido.getEstrategiaEmparejamiento().calcularCompatibilidad(usuario, partido) : 0.0;
        NivelJuego nivelMinimo = null;
        NivelJuego nivelMaximo = null;
        if ("POR_NIVEL".equals(partido.getEstrategiaActual()) && partido.getEstrategiaEmparejamiento() instanceof com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorNivelStrategy) {
            com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorNivelStrategy estrategia = (com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorNivelStrategy) partido.getEstrategiaEmparejamiento();
            try {
                java.lang.reflect.Field fMin = estrategia.getClass().getDeclaredField("nivelMinimo");
                java.lang.reflect.Field fMax = estrategia.getClass().getDeclaredField("nivelMaximo");
                fMin.setAccessible(true);
                fMax.setAccessible(true);
                nivelMinimo = (NivelJuego) fMin.get(estrategia);
                nivelMaximo = (NivelJuego) fMax.get(estrategia);
            } catch (Exception e) {
                // Ignorar si falla
            }
        }
        return PartidoResponse.builder()
                .id(partido.getId())
                .deporte(mapearDeporteAResponse(partido.getDeporte()))
                .cantidadJugadoresRequeridos(partido.getCantidadJugadoresRequeridos())
                .cantidadJugadoresActual(partido.getParticipantes().size())
                .duracion(partido.getDuracion())
                .ubicacion(mapearUbicacionAResponse(partido.getUbicacion()))
                .horario(partido.getHorario())
                .organizador(mapearUsuarioAResponse(partido.getOrganizador()))
                .jugadores(partido.getParticipantes().stream()
                        .map(this::mapearUsuarioAResponse)
                        .collect(Collectors.toList()))
                .estado(partido.getEstadoActual())
                .estrategiaEmparejamiento(partido.getEstrategiaActual())
                .createdAt(partido.getCreatedAt())
                .puedeUnirse(puedeUnirse)
                .compatibilidad(compatibilidad)
                .nivelMinimo(nivelMinimo)
                .nivelMaximo(nivelMaximo)
                .build();
    }

    private DeporteResponse mapearDeporteAResponse(Deporte deporte) {
        return DeporteResponse.builder()
                .id(deporte.getId())
                .nombre(deporte.getNombre())
                .jugadoresPorEquipo(deporte.getJugadoresPorEquipo())
                .reglasBasicas(deporte.getReglasBasicas())
                .build();
    }

    private UbicacionResponse mapearUbicacionAResponse(Ubicacion ubicacion) {
        return UbicacionResponse.builder()
                .id(ubicacion.getId())
                .direccion(ubicacion.getDireccion())
                .latitud(ubicacion.getLatitud())
                .longitud(ubicacion.getLongitud())
                .zona(ubicacion.getZona())
                .build();
    }

    private UsuarioResponse mapearUsuarioAResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombreUsuario(usuario.getNombreUsuario())
                .email(usuario.getEmail())
                .deporteFavorito(usuario.getDeporteFavorito() != null ? usuario.getDeporteFavorito().getNombre() : null)
                .nivelJuego(usuario.getNivelJuego())
                .role(usuario.getRole())
                .activo(usuario.isActivo())
                .createdAt(usuario.getCreatedAt())
                .build();
    }

    public List<PartidoResponse> buscarTodosParaAdmin() {
        List<Partido> partidos = partidoRepository.findAll();
        return partidos.stream()
                .map(p -> mapearAResponse(p, null))
                .collect(Collectors.toList());
    }
}