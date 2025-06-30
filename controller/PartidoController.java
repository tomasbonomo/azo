package com.uade.tpo.deportes.controller;
import com.uade.tpo.deportes.service.confirmacion.ConfirmacionService;
import com.uade.tpo.deportes.service.comentarios.ComentarioService;
import com.uade.tpo.deportes.dto.AgregarComentarioRequest;
import com.uade.tpo.deportes.dto.*;
import com.uade.tpo.deportes.entity.Usuario;
import com.uade.tpo.deportes.entity.Partido;
import com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorHistorialStrategy;
import com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorNivelStrategy;
import com.uade.tpo.deportes.patterns.strategy.EmparejamientoPorCercaniaStrategy;
import com.uade.tpo.deportes.repository.PartidoRepository;
import com.uade.tpo.deportes.service.partido.PartidoService;
import com.uade.tpo.deportes.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/partidos")
@RequiredArgsConstructor
public class PartidoController {

    private final PartidoService partidoService;
    private final UsuarioService usuarioService;

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private ConfirmacionService confirmacionService;
    @Autowired
    private ComentarioService comentarioService;

    @Autowired
    private EmparejamientoPorHistorialStrategy emparejamientoPorHistorial;
    @Autowired
    private EmparejamientoPorNivelStrategy emparejamientoPorNivel;
    @Autowired
    private EmparejamientoPorCercaniaStrategy emparejamientoPorCercania;

    private static final Logger logger = LoggerFactory.getLogger(PartidoController.class);

    // AGREGAR ESTOS MÉTODOS AL FINAL:
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<MessageResponse> confirmarParticipacion(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id) {
        MessageResponse response = confirmacionService.confirmarParticipacion(usuario.getEmail(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comentar")
    public ResponseEntity<MessageResponse> agregarComentario(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id,
            @RequestBody AgregarComentarioRequest request) {
        request.setPartidoId(id);
        MessageResponse response = comentarioService.agregarComentario(usuario.getEmail(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PartidoResponse> crearPartido(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody CrearPartidoRequest request) {
        PartidoResponse response = partidoService.crearPartido(usuario.getEmail(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidoResponse> obtenerPartido(@PathVariable Long id) {
        PartidoResponse response = partidoService.obtenerPartido(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mis-partidos")
    public ResponseEntity<List<PartidoResponse>> obtenerMisPartidos(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            logger.debug("[DEBUG] Usuario autenticado en /mis-partidos: null");
            return ResponseEntity.status(401).build();
        }
        Usuario usuario;
        if (principal instanceof Usuario) {
            usuario = (Usuario) principal;
            logger.debug("[DEBUG] Principal es Usuario: {} (rol: {})", usuario.getEmail(), usuario.getRole());
        } else if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            usuario = usuarioService.obtenerUsuarioPorEmail(email);
            logger.debug("[DEBUG] Principal es UserDetails: {} (rol: {})", usuario.getEmail(), usuario.getRole());
        } else {
            logger.warn("[DEBUG] Principal de tipo inesperado: {}", principal.getClass().getName());
            return ResponseEntity.status(401).build();
        }
        List<PartidoResponse> partidos = partidoService.obtenerPartidosDelUsuario(usuario.getEmail());
        return ResponseEntity.ok(partidos);
    }

    @PostMapping("/buscar")
    public ResponseEntity<Page<PartidoResponse>> buscarPartidos(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody CriteriosBusqueda criterios,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PartidoResponse> partidos = partidoService.buscarPartidos(usuario.getEmail(), criterios, pageable);
        return ResponseEntity.ok(partidos);
    }

    @PostMapping("/{id}/unirse")
    public ResponseEntity<MessageResponse> unirseAPartido(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id) {
        MessageResponse response = partidoService.unirseAPartido(usuario.getEmail(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<MessageResponse> cambiarEstadoPartido(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id,
            @RequestBody CambiarEstadoPartidoRequest request) {
        MessageResponse response = partidoService.cambiarEstadoPartido(usuario.getEmail(), id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/estrategia")
    public ResponseEntity<MessageResponse> configurarEstrategia(
            @PathVariable Long id,
            @RequestBody ConfigurarEstrategiaRequest request) {
        MessageResponse response = partidoService.configurarEstrategia(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/todos")
    public ResponseEntity<List<PartidoResponse>> obtenerTodosLosPartidos(@AuthenticationPrincipal Usuario usuario) {
        if (!usuario.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        List<PartidoResponse> partidos = partidoService.buscarTodosParaAdmin();
        return ResponseEntity.ok(partidos);
    }

    @GetMapping("/{id}/comentarios")
    public ResponseEntity<Page<ComentarioResponse>> obtenerComentariosPartido(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ComentarioResponse> comentarios = comentarioService.obtenerComentariosPartido(id, pageable);
        return ResponseEntity.ok(comentarios);
    }

    @GetMapping("/admin/todos")
    //@PreAuthorize("hasRole('ADMIN')") // Descomenta si tienes seguridad por roles
    public List<PartidoResponse> getAllPartidosAdmin() {
        return partidoService.buscarTodosParaAdmin();
    }

    @PostMapping("/{partidoId}/test-historial")
    public ResponseEntity<Map<String, Object>> testEstrategiaHistorial(
            @PathVariable Long partidoId,
            @RequestParam String emailUsuario) {
        try {
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
            Partido partido = partidoService.obtenerPartidoPorId(partidoId);
            
            // Configurar estrategia por historial
            partido.setEstrategiaActual("POR_HISTORIAL");
            partidoService.configurarEstrategia(partidoId, 
                ConfigurarEstrategiaRequest.builder()
                    .tipoEstrategia("POR_HISTORIAL")
                    .build());
            
            // Obtener la estrategia configurada
            EmparejamientoPorHistorialStrategy estrategia = 
                (EmparejamientoPorHistorialStrategy) partido.getEstrategiaEmparejamiento();
            
            // Calcular compatibilidad
            Double compatibilidad = estrategia.calcularCompatibilidad(usuario, partido);
            boolean puedeUnirse = estrategia.puedeUnirse(usuario, partido);
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("usuario", usuario.getNombreUsuario());
            resultado.put("partidoId", partidoId);
            resultado.put("deporte", partido.getDeporte().getNombre());
            resultado.put("puedeUnirse", puedeUnirse);
            resultado.put("compatibilidad", compatibilidad);
            resultado.put("compatibilidadPorcentaje", String.format("%.1f%%", compatibilidad * 100));
            resultado.put("estrategia", "POR_HISTORIAL");
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error probando estrategia por historial");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/test-historial-directo")
    public ResponseEntity<Map<String, Object>> testEstrategiaHistorialDirecto(
            @RequestParam String emailUsuario,
            @RequestParam Long partidoId) {
        try {
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
            Partido partido = partidoService.obtenerPartidoPorId(partidoId);
            
            // Crear instancia directa de la estrategia
            EmparejamientoPorHistorialStrategy estrategia = new EmparejamientoPorHistorialStrategy();
            
            // Inyectar el repositorio manualmente (esto es solo para testing)
            try {
                java.lang.reflect.Field repoField = EmparejamientoPorHistorialStrategy.class.getDeclaredField("partidoRepository");
                repoField.setAccessible(true);
                repoField.set(estrategia, partidoRepository);
            } catch (Exception e) {
                System.err.println("No se pudo inyectar el repositorio: " + e.getMessage());
            }
            
            // Calcular compatibilidad directamente
            Double compatibilidad = estrategia.calcularCompatibilidad(usuario, partido);
            boolean puedeUnirse = estrategia.puedeUnirse(usuario, partido);
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("usuario", usuario.getNombreUsuario());
            resultado.put("partidoId", partidoId);
            resultado.put("deporte", partido.getDeporte().getNombre());
            resultado.put("puedeUnirse", puedeUnirse);
            resultado.put("compatibilidad", compatibilidad);
            resultado.put("compatibilidadPorcentaje", String.format("%.1f%%", compatibilidad * 100));
            resultado.put("estrategia", "POR_HISTORIAL_DIRECTO");
            resultado.put("metodo", "Inyección manual para testing");
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error probando estrategia por historial directo");
            error.put("mensaje", e.getMessage());
            error.put("stackTrace", e.getStackTrace());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/diagnostico-usuario/{emailUsuario}")
    public ResponseEntity<Map<String, Object>> diagnosticoUsuario(@PathVariable String emailUsuario) {
        try {
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
            
            // Obtener estadísticas del usuario
            List<Partido> partidosOrganizados = partidoRepository.findByOrganizador(usuario);
            List<Partido> partidosJugados = partidoRepository.findPartidosConJugador(usuario);
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("usuario", usuario.getNombreUsuario());
            resultado.put("email", usuario.getEmail());
            resultado.put("nivelJuego", usuario.getNivelJuego());
            resultado.put("deporteFavorito", usuario.getDeporteFavorito() != null ? usuario.getDeporteFavorito().getNombre() : "No especificado");
            resultado.put("partidosOrganizados", partidosOrganizados.size());
            resultado.put("partidosJugados", partidosJugados.size());
            resultado.put("totalPartidos", partidosOrganizados.size() + partidosJugados.size());
            
            // Obtener deportes jugados
            Set<String> deportesJugados = partidosJugados.stream()
                    .map(p -> p.getDeporte().getNombre())
                    .collect(java.util.stream.Collectors.toSet());
            partidosOrganizados.stream()
                    .map(p -> p.getDeporte().getNombre())
                    .forEach(deportesJugados::add);
            
            resultado.put("deportesJugados", deportesJugados);
            
            // Obtener jugadores conocidos
            Set<Long> jugadoresConocidos = partidosJugados.stream()
                    .flatMap(p -> p.getParticipantes().stream())
                    .filter(j -> !j.getId().equals(usuario.getId()))
                    .map(Usuario::getId)
                    .collect(java.util.stream.Collectors.toSet());
            
            partidosJugados.stream()
                    .map(p -> p.getOrganizador().getId())
                    .filter(id -> !id.equals(usuario.getId()))
                    .forEach(jugadoresConocidos::add);
            
            resultado.put("jugadoresConocidos", jugadoresConocidos.size());
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error en diagnóstico de usuario");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{partidoId}/compatibilidad")
    public ResponseEntity<Map<String, Object>> calcularCompatibilidadPorEstrategia(
            @PathVariable Long partidoId,
            @RequestParam String emailUsuario,
            @RequestParam String estrategia) {
        try {
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(emailUsuario);
            Partido partido = partidoService.obtenerPartidoPorId(partidoId);

            // Seleccionar la estrategia a usar
            double compatibilidad = 0.0;
            String estrategiaUsada = estrategia.toUpperCase();
            switch (estrategiaUsada) {
                case "POR_HISTORIAL":
                    compatibilidad = emparejamientoPorHistorial.calcularCompatibilidad(usuario, partido);
                    break;
                case "POR_NIVEL":
                    compatibilidad = emparejamientoPorNivel.calcularCompatibilidad(usuario, partido);
                    break;
                case "POR_CERCANIA":
                    compatibilidad = emparejamientoPorCercania.calcularCompatibilidad(usuario, partido);
                    break;
                default:
                    compatibilidad = emparejamientoPorNivel.calcularCompatibilidad(usuario, partido);
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("partidoId", partidoId);
            resultado.put("usuario", usuario.getNombreUsuario());
            resultado.put("estrategia", estrategiaUsada);
            resultado.put("compatibilidad", compatibilidad);
            resultado.put("compatibilidadPorcentaje", String.format("%.1f%%", compatibilidad * 100));
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error calculando compatibilidad por estrategia");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}