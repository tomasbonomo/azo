package com.uade.tpo.deportes.service.pushtoken;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import com.uade.tpo.deportes.entity.PushToken;
import com.uade.tpo.deportes.entity.Usuario;
import com.uade.tpo.deportes.repository.PushTokenRepository;
import com.uade.tpo.deportes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

/**
 * Servicio TEMPORAL para tokens push - Implementación en memoria
 * TODO: Migrar a base de datos cuando esté listo
 */
@Service
public class PushTokenService {

    @Autowired
    private PushTokenRepository pushTokenRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Registrar un nuevo token push para un usuario
     */
    public void registrarToken(Long usuarioId, String token, String deviceType) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            System.err.println("[PushTokenService] Usuario no encontrado para ID: " + usuarioId);
            return;
        }
        // Buscar si ya existe el token
        Optional<PushToken> existente = pushTokenRepository.findByToken(token);
        if (existente.isPresent()) {
            PushToken t = existente.get();
            t.setActivo(true);
            t.setUsuario(usuario); // Opcional: reasignar usuario si cambió
            t.setDeviceType(deviceType);
            pushTokenRepository.save(t);
            System.out.println("✅ Token push reactivado para usuario " + usuarioId + ": " + deviceType);
            return;
        }
        // Guardar nuevo token
        PushToken pushToken = PushToken.builder()
                .usuario(usuario)
                .token(token)
                .deviceType(deviceType)
                .activo(true)
                .build();
        pushTokenRepository.save(pushToken);
        System.out.println("✅ Token push registrado (en base de datos) para usuario " + usuarioId + ": " + deviceType);
    }

    /**
     * Obtener todos los tokens activos de un usuario
     */
    public List<String> obtenerTokensUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return new ArrayList<>();
        List<PushToken> tokens = pushTokenRepository.findByUsuarioAndActivoTrue(usuario);
        List<String> result = new ArrayList<>();
        for (PushToken t : tokens) {
            result.add(t.getToken());
        }
        return result;
    }

    /**
     * Obtener tokens de usuarios con un deporte favorito específico
     * TODO: Implementar cuando tengamos la entidad
     */
    public List<String> obtenerTokensPorDeporteFavorito(String tipoDeporte) {
        System.out.println("⚠️ obtenerTokensPorDeporteFavorito - Implementación temporal");
        return new ArrayList<>(); // Por ahora vacío
    }

    /**
     * Desactivar un token (cuando falla el envío o el usuario se desconecta)
     */
    public void desactivarToken(String token) {
        pushTokenRepository.findByToken(token).ifPresent(t -> {
            t.setActivo(false);
            pushTokenRepository.save(t);
        });
        System.out.println("⚠️ Token desactivado (en base de datos): " + token);
    }

    /**
     * Eliminar tokens antiguos (limpieza periódica)
     */
    public void limpiarTokensAntiguos() {
        // Implementación opcional
        System.out.println("🧹 Limpieza de tokens - Implementación temporal");
    }

    /**
     * Actualizar fecha de último uso de un token
     */
    public void actualizarUltimoUso(String token) {
        System.out.println("📅 actualizarUltimoUso - Implementación temporal para: " + token);
        // Por ahora no hacer nada
    }

    /**
     * Obtener estadísticas de tokens
     */
    public TokenStats obtenerEstadisticas() {
        long totalTokens = pushTokenRepository.count();
        long usuariosConTokens = pushTokenRepository.findAll().stream().map(PushToken::getUsuario).distinct().count();
        long tokensActivos = pushTokenRepository.findAll().stream().filter(PushToken::isActivo).count();
        return TokenStats.builder()
            .totalTokens(totalTokens)
            .tokensActivos(tokensActivos)
            .usuariosConTokens(usuariosConTokens)
            .build();
    }

    /**
     * Datos de estadísticas - CLASE INTERNA SIMPLIFICADA
     */
    public static class TokenStats {
        private long totalTokens;
        private long tokensActivos;
        private long usuariosConTokens;

        // Constructor vacío
        public TokenStats() {}

        // Constructor con parámetros
        public TokenStats(long totalTokens, long tokensActivos, long usuariosConTokens) {
            this.totalTokens = totalTokens;
            this.tokensActivos = tokensActivos;
            this.usuariosConTokens = usuariosConTokens;
        }

        public static TokenStatsBuilder builder() {
            return new TokenStatsBuilder();
        }

        // Getters
        public long getTotalTokens() { return totalTokens; }
        public long getTokensActivos() { return tokensActivos; }
        public long getUsuariosConTokens() { return usuariosConTokens; }

        // Setters
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
        public void setTokensActivos(long tokensActivos) { this.tokensActivos = tokensActivos; }
        public void setUsuariosConTokens(long usuariosConTokens) { this.usuariosConTokens = usuariosConTokens; }

        public static class TokenStatsBuilder {
            private long totalTokens;
            private long tokensActivos;
            private long usuariosConTokens;

            public TokenStatsBuilder totalTokens(long totalTokens) {
                this.totalTokens = totalTokens;
                return this;
            }

            public TokenStatsBuilder tokensActivos(long tokensActivos) {
                this.tokensActivos = tokensActivos;
                return this;
            }

            public TokenStatsBuilder usuariosConTokens(long usuariosConTokens) {
                this.usuariosConTokens = usuariosConTokens;
                return this;
            }

            public TokenStats build() {
                return new TokenStats(totalTokens, tokensActivos, usuariosConTokens);
            }
        }
    }
}