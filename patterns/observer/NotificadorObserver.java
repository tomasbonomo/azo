package com.uade.tpo.deportes.patterns.observer;

import com.uade.tpo.deportes.entity.Partido;
import com.uade.tpo.deportes.entity.Usuario;
import com.uade.tpo.deportes.enums.EventoPartido;
import com.uade.tpo.deportes.patterns.adapter.NotificadorEmail;
import com.uade.tpo.deportes.patterns.adapter.NotificadorPush;
import com.uade.tpo.deportes.service.pushtoken.PushTokenService;
import com.uade.tpo.deportes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observer general para notificar a los participantes y organizador
 * en los eventos relevantes del partido (excepto creación).
 * 
 * PATRONES:
 * - Observer: para recibir eventos de cambio de estado.
 * - Adapter: para unificar notificación por email y push.
 */
@Component
public class NotificadorObserver implements ObserverPartido {

    @Autowired
    private NotificadorEmail notificadorEmail;
    
    @Autowired
    private NotificadorPush notificadorPush;
    
    @Autowired
    private PushTokenService pushTokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Cache para evitar notificaciones duplicadas
    // Key: partidoId + evento
    private final ConcurrentHashMap<String, LocalDateTime> notificacionesEnviadas = new ConcurrentHashMap<>();
    private final long CACHE_EXPIRY_MINUTES = 30;
    private final long DEDUPLICATION_WINDOW_MINUTES = 2;

    @Override
    public void actualizar(Partido partido, EventoPartido evento) {
        // Crear clave única para este evento
        String cacheKey = partido.getId() + "_" + evento.name();
        LocalDateTime ahora = LocalDateTime.now();
        // Verificar si ya notificamos este evento recientemente
        LocalDateTime ultimaNotificacion = notificacionesEnviadas.get(cacheKey);
        if (ultimaNotificacion != null &&
            ultimaNotificacion.plusMinutes(DEDUPLICATION_WINDOW_MINUTES).isAfter(ahora)) {
            System.out.println("⚠️ Notificación duplicada evitada para partido " + partido.getId() + " evento " + evento);
            return;
        }
        // Marcar como notificado ANTES de procesar para evitar concurrencia
        notificacionesEnviadas.put(cacheKey, ahora);
        limpiarCacheExpirado();

        System.out.println("🔔 === NOTIFICADOR OBSERVER UNIFICADO ===");
        System.out.println("   Evento recibido: " + evento);
        System.out.println("   Partido ID: " + partido.getId());
        System.out.println("   Deporte: " + partido.getDeporte().getNombre());
        
        Set<Long> idsNotificados = new HashSet<>();
        List<Usuario> usuariosANotificar = new ArrayList<>();

        if (evento == EventoPartido.PARTIDO_CREADO) {
            // Notificar a todos los usuarios cuyo deporte favorito coincide (excepto admin)
            usuariosANotificar = usuarioRepository.findByDeporteFavoritoAndActivoTrue(partido.getDeporte())
                .stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole().name()))
                .collect(Collectors.toList());
        } else if (
            evento == EventoPartido.JUGADOR_UNIDO ||
            evento == EventoPartido.PARTIDO_ARMADO ||
            evento == EventoPartido.PARTIDO_CONFIRMADO ||
            evento == EventoPartido.PARTIDO_INICIADO ||
            evento == EventoPartido.PARTIDO_FINALIZADO ||
            evento == EventoPartido.PARTIDO_CANCELADO
        ) {
            // Notificar solo a participantes y organizador (excepto admin)
            usuariosANotificar.addAll(partido.getParticipantes());
            if (partido.getOrganizador() != null) {
                usuariosANotificar.add(partido.getOrganizador());
            }
            usuariosANotificar = usuariosANotificar.stream()
                .filter(u -> u != null)
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole().name()))
                .collect(Collectors.toList());
        } else {
            System.out.println("⚠️ Evento ignorado: " + evento);
            return;
        }

        String mensaje = construirMensaje(partido, evento);
        usuariosANotificar.stream()
            .filter(u -> idsNotificados.add(u.getId())) // evitar duplicados
            .forEach(usuario -> {
                try {
                    notificadorEmail.enviarNotificacion(usuario.getEmail(), mensaje);
                    var tokensPush = pushTokenService.obtenerTokensUsuario(usuario.getId());
                    if (!tokensPush.isEmpty()) {
                        tokensPush.forEach(token -> {
                            try {
                                notificadorPush.enviarNotificacionPush(token, mensaje);
                                System.out.println("✅ Push notification enviada a usuario " + usuario.getId() + " con token: " + token.substring(0, Math.min(20, token.length())) + "...");
                            } catch (Exception e) {
                                System.err.println("❌ Error enviando push a usuario " + usuario.getId() + ": " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error enviando notificación a " + usuario.getEmail() + ": " + e.getMessage());
                }
            });
    }

    private String construirMensaje(Partido partido, EventoPartido evento) {
        String deporte = partido.getDeporte().getNombre();
        String ubicacion = partido.getUbicacion().getDireccion();
        switch (evento) {
            case PARTIDO_CREADO:
                return String.format("Se creó un nuevo partido de %s.", deporte);
            case JUGADOR_UNIDO:
                return String.format("Se unió un nuevo jugador al partido de %s en %s.", deporte, ubicacion);
            case PARTIDO_ARMADO:
                return String.format("¡Partido de %s completo! Esperando confirmación.", deporte);
            case PARTIDO_CONFIRMADO:
                return String.format("Partido de %s confirmado para %s en %s.", deporte, partido.getHorario().toString(), ubicacion);
            case PARTIDO_INICIADO:
                return String.format("¡El partido de %s ha comenzado!", deporte);
            case PARTIDO_FINALIZADO:
                return String.format("El partido de %s ha finalizado. ¡Gracias por participar!", deporte);
            case PARTIDO_CANCELADO:
                return String.format("El partido de %s en %s ha sido cancelado.", deporte, ubicacion);
            default:
                return String.format("Actualización en el partido de %s.", deporte);
        }
    }

    private void limpiarCacheExpirado() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(CACHE_EXPIRY_MINUTES);
        notificacionesEnviadas.entrySet().removeIf(entry ->
            entry.getValue().isBefore(limite)
        );
    }
}