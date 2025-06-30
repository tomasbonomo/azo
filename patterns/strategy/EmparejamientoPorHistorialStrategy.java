package com.uade.tpo.deportes.patterns.strategy;

import com.uade.tpo.deportes.entity.Partido;
import com.uade.tpo.deportes.entity.Usuario;
import com.uade.tpo.deportes.repository.PartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 📊 ESTRATEGIA POR HISTORIAL - ALGORITMO INTELIGENTE SIMPLE
 * 
 * ALGORITMO:
 * - Sin historial = 60% compatibilidad (neutral)
 * - Historial positivo = hasta 95% compatibilidad
 * - Jugadores conocidos = +20% bonus
 * - Deportes similares jugados = +10% bonus
 * - Usuario activo = +15% bonus
 */
@Component
public class EmparejamientoPorHistorialStrategy implements EstrategiaEmparejamiento {

    @Autowired
    private PartidoRepository partidoRepository;

    @Override
    public boolean puedeUnirse(Usuario usuario, Partido partido) {
        System.out.println("🔍 === VERIFICANDO SI PUEDE UNIRSE (HISTORIAL) ===");
        System.out.println("   Usuario: " + usuario.getNombreUsuario() + " (ID: " + usuario.getId() + ")");
        System.out.println("   Partido: " + partido.getId() + " - " + partido.getDeporte().getNombre());
        System.out.println("   Repositorio inyectado: " + (partidoRepository != null ? "SÍ" : "NO"));
        
        // Verificar que el repositorio esté disponible
        if (partidoRepository == null) {
            System.err.println("❌ ERROR: PartidoRepository no está inyectado");
            return false;
        }
        
        // Verificaciones básicas
        if (partido.getParticipantes().size() >= partido.getCantidadJugadoresRequeridos()) {
            System.out.println("   ❌ Partido lleno");
            return false;
        }
        
        if (partido.getParticipantes().contains(usuario)) {
            System.out.println("   ❌ Usuario ya participa");
            return false;
        }

        // Permitir que el organizador también pueda unirse como jugador
        // (ya no se impide si usuario es el organizador)

        // ✨ VERIFICACIÓN INTELIGENTE POR HISTORIAL
        boolean resultado = tieneHistorialCompatible(usuario, partido);
        System.out.println("   ✅ Resultado final: " + resultado);
        return resultado;
    }

    @Override
    public Double calcularCompatibilidad(Usuario usuario, Partido partido) {
        System.out.println("🧮 === CALCULANDO COMPATIBILIDAD (HISTORIAL) ===");
        System.out.println("   Usuario: " + usuario.getNombreUsuario());
        System.out.println("   Partido: " + partido.getId());
        
        if (!puedeUnirse(usuario, partido)) {
            System.out.println("   ❌ No puede unirse - compatibilidad 0");
            return 0.0;
        }

        String deportePartido = partido.getDeporte().getNombre().toUpperCase();
        // Contar partidos jugados por deporte (solo los que jugó)
        Map<String, Integer> partidosPorDeporte = new HashMap<>();
        try {
            partidoRepository.findPartidosConJugador(usuario).forEach(p -> {
                String dep = p.getDeporte().getNombre().toUpperCase();
                partidosPorDeporte.put(dep, partidosPorDeporte.getOrDefault(dep, 0) + 1);
            });
        } catch (Exception e) {
            // Ignorar
        }
        System.out.println("   Partidos jugados por deporte: " + partidosPorDeporte);

        // Ranking solo con deportes jugados (cantidad > 0)
        List<Map.Entry<String, Integer>> ranking = new ArrayList<>(
            partidosPorDeporte.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList()
        );
        System.out.println("   Ranking de deportes jugados: " + ranking);

        // Buscar la posición del deporte del partido
        int posicion = -1;
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).getKey().equals(deportePartido)) {
                posicion = i;
                break;
            }
        }

        double compatibilidad;
        if (posicion == 0) {
            compatibilidad = 1.0;
            System.out.println("   ✅ Es el deporte más jugado. Compatibilidad: 100%");
        } else if (posicion == 1) {
            compatibilidad = 0.85;
            System.out.println("   2° más jugado. Compatibilidad: 85%");
        } else if (posicion == 2) {
            compatibilidad = 0.75;
            System.out.println("   3° más jugado. Compatibilidad: 75%");
        } else if (posicion == 3) {
            compatibilidad = 0.70;
            System.out.println("   4° más jugado. Compatibilidad: 70%");
        } else if (posicion == 4) {
            compatibilidad = 0.65;
            System.out.println("   5° más jugado. Compatibilidad: 65%");
        } else if (posicion == 5) {
            compatibilidad = 0.60;
            System.out.println("   6° más jugado. Compatibilidad: 60%");
        } else if (posicion == 6) {
            compatibilidad = 0.55;
            System.out.println("   7° más jugado. Compatibilidad: 55%");
        } else if (posicion >= 7) {
            compatibilidad = 0.50;
            System.out.println("   8° o más. Compatibilidad: 50%");
        } else {
            // Nunca jugado
            compatibilidad = 0.50;
            System.out.println("   ⚠️ Nunca jugó este deporte. Compatibilidad: 50%");
        }

        System.out.println("📊 Compatibilidad historial " + usuario.getNombreUsuario() + " → " +
                         String.format("%.1f%% (Ranking: %d)",
                         compatibilidad * 100, posicion >= 0 ? posicion + 1 : -1));
        
        return compatibilidad;
    }

    // 📊 COMPATIBILIDAD BASE POR EXPERIENCIA
    private double calcularCompatibilidadBase(Usuario usuario) {
        try {
            System.out.println("   📊 Calculando compatibilidad base para: " + usuario.getNombreUsuario());
            
            // Contar partidos del usuario (organizados + jugados)
            List<Partido> partidosOrganizados = partidoRepository.findByOrganizador(usuario);
            List<Partido> partidosJugados = partidoRepository.findPartidosConJugador(usuario);
            
            System.out.println("   📊 Partidos organizados: " + partidosOrganizados.size());
            System.out.println("   📊 Partidos jugados: " + partidosJugados.size());
            
            int totalPartidos = partidosOrganizados.size() + partidosJugados.size();
            System.out.println("   📊 Total partidos: " + totalPartidos);
            
            // Algoritmo de experiencia
            double compatibilidad;
            if (totalPartidos == 0) {
                compatibilidad = 0.6; // Nuevo usuario - neutral
            } else if (totalPartidos <= 2) {
                compatibilidad = 0.65; // Principiante
            } else if (totalPartidos <= 5) {
                compatibilidad = 0.75; // Con algo de experiencia
            } else if (totalPartidos <= 10) {
                compatibilidad = 0.85; // Experimentado
            } else {
                compatibilidad = 0.9; // Veterano
            }
            
            System.out.println("   📊 Compatibilidad base calculada: " + String.format("%.1f%%", compatibilidad * 100));
            return compatibilidad;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando historial base: " + e.getMessage());
            e.printStackTrace();
            return 0.6; // Valor por defecto
        }
    }

    // 🤝 BONUS POR JUGADORES CONOCIDOS
    private double calcularBonusJugadoresConocidos(Usuario usuario, Partido partido) {
        try {
            System.out.println("   🤝 Calculando bonus por jugadores conocidos");
            
            // Obtener jugadores con los que ya jugó
            Set<Long> jugadoresConocidos = obtenerJugadoresConocidos(usuario);
            System.out.println("   🤝 Jugadores conocidos totales: " + jugadoresConocidos.size());
            
            // Contar cuántos jugadores del partido ya conoce
            long jugadoresConocidosEnPartido = partido.getParticipantes().stream()
                    .mapToLong(j -> jugadoresConocidos.contains(j.getId()) ? 1 : 0)
                    .sum();
            
            // También verificar organizador
            if (jugadoresConocidos.contains(partido.getOrganizador().getId())) {
                jugadoresConocidosEnPartido++;
            }
            
            System.out.println("   🤝 Jugadores conocidos en este partido: " + jugadoresConocidosEnPartido);
            
            // Bonus proporcional
            double bonus;
            if (jugadoresConocidosEnPartido == 0) {
                bonus = 0.0;
            } else if (jugadoresConocidosEnPartido == 1) {
                bonus = 0.1; // 10% bonus por 1 conocido
            } else if (jugadoresConocidosEnPartido == 2) {
                bonus = 0.15; // 15% bonus por 2 conocidos
            } else {
                bonus = 0.2; // 20% bonus por 3+ conocidos
            }
            
            System.out.println("   🤝 Bonus por conocidos: " + String.format("%.1f%%", bonus * 100));
            return bonus;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando jugadores conocidos: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    // 🏀 BONUS POR DEPORTES SIMILARES
    private double calcularBonusDeportesSimilares(Usuario usuario, Partido partido) {
        try {
            System.out.println("   🏀 Calculando bonus por deportes similares");
            
            // Obtener deportes que ya jugó
            Set<String> deportesJugados = obtenerDeportesJugados(usuario);
            System.out.println("   🏀 Deportes jugados: " + deportesJugados);
            
            String deportePartido = partido.getDeporte().getNombre().toUpperCase();
            System.out.println("   🏀 Deporte del partido: " + deportePartido);
            
            // Si ya jugó este deporte exacto
            if (deportesJugados.contains(deportePartido)) {
                System.out.println("   🏀 Bonus por deporte exacto: 15%");
                return 0.15; // 15% bonus por experiencia en el deporte
            }
            
            // Si jugó deportes similares
            boolean jugoDeporteSimilar = false;
            if (deportePartido.equals("FUTBOL")) {
                jugoDeporteSimilar = deportesJugados.contains("VOLEY");
            } else if (deportePartido.equals("BASQUET")) {
                jugoDeporteSimilar = deportesJugados.contains("VOLEY") || deportesJugados.contains("TENIS");
            } else if (deportePartido.equals("VOLEY")) {
                jugoDeporteSimilar = deportesJugados.contains("FUTBOL") || deportesJugados.contains("BASQUET");
            } else if (deportePartido.equals("TENIS")) {
                jugoDeporteSimilar = deportesJugados.contains("BASQUET");
            }
            
            double bonus = jugoDeporteSimilar ? 0.08 : 0.0; // 8% bonus por deporte similar
            System.out.println("   🏀 Bonus por deporte similar: " + String.format("%.1f%%", bonus * 100));
            return bonus;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando deportes similares: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    // ⚡ BONUS POR ACTIVIDAD RECIENTE
    private double calcularBonusActividad(Usuario usuario) {
        try {
            System.out.println("   ⚡ Calculando bonus por actividad reciente");
            
            LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
            
            // Contar partidos recientes
            List<Partido> partidosRecientes = partidoRepository.findByOrganizador(usuario).stream()
                    .filter(p -> p.getCreatedAt().isAfter(hace30Dias))
                    .collect(Collectors.toList());
            
            List<Partido> jugadosRecientes = partidoRepository.findPartidosConJugador(usuario).stream()
                    .filter(p -> p.getCreatedAt().isAfter(hace30Dias))
                    .collect(Collectors.toList());
            
            int partidosRecientesTotales = partidosRecientes.size() + jugadosRecientes.size();
            System.out.println("   ⚡ Partidos recientes (30 días): " + partidosRecientesTotales);
            
            // Bonus por actividad
            double bonus;
            if (partidosRecientesTotales >= 3) {
                bonus = 0.15; // Muy activo
            } else if (partidosRecientesTotales >= 1) {
                bonus = 0.08; // Moderadamente activo
            } else {
                bonus = 0.0; // Sin actividad reciente
            }
            
            System.out.println("   ⚡ Bonus por actividad: " + String.format("%.1f%%", bonus * 100));
            return bonus;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando actividad: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    // 🔍 VERIFICAR HISTORIAL COMPATIBLE
    private boolean tieneHistorialCompatible(Usuario usuario, Partido partido) {
        try {
            System.out.println("   🔍 Verificando historial compatible");
            
            // Por ahora, siempre permitir (en implementación real verificarías problemas)
            // Aquí podrías verificar:
            // - No hay reportes negativos con otros jugadores
            // - No hay cancelaciones frecuentes
            // - No hay comportamiento problemático
            
            System.out.println("   🔍 Historial compatible: true");
            return true;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error verificando historial: " + e.getMessage());
            e.printStackTrace();
            return true; // Por defecto permitir
        }
    }

    // 🤝 OBTENER JUGADORES CONOCIDOS
    private Set<Long> obtenerJugadoresConocidos(Usuario usuario) {
        try {
            System.out.println("   🤝 Obteniendo jugadores conocidos para: " + usuario.getNombreUsuario());
            
            Set<Long> jugadoresConocidos = partidoRepository.findPartidosConJugador(usuario).stream()
                    .flatMap(p -> p.getParticipantes().stream())
                    .filter(j -> !j.getId().equals(usuario.getId()))
                    .map(Usuario::getId)
                    .collect(Collectors.toSet());
            
            System.out.println("   🤝 Jugadores conocidos de partidos jugados: " + jugadoresConocidos.size());
            
            // También agregar organizadores de partidos donde participó
            partidoRepository.findPartidosConJugador(usuario).stream()
                    .map(p -> p.getOrganizador().getId())
                    .filter(id -> !id.equals(usuario.getId()))
                    .forEach(jugadoresConocidos::add);
            
            System.out.println("   🤝 Total jugadores conocidos (incluyendo organizadores): " + jugadoresConocidos.size());
            return jugadoresConocidos;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo jugadores conocidos: " + e.getMessage());
            e.printStackTrace();
            return Set.of();
        }
    }

    // 🏀 OBTENER DEPORTES JUGADOS
    private Set<String> obtenerDeportesJugados(Usuario usuario) {
        try {
            System.out.println("   🏀 Obteniendo deportes jugados para: " + usuario.getNombreUsuario());
            
            Set<String> deportes = partidoRepository.findPartidosConJugador(usuario).stream()
                    .map(p -> p.getDeporte().getNombre().toUpperCase())
                    .collect(Collectors.toSet());
            
            System.out.println("   🏀 Deportes de partidos jugados: " + deportes);
            
            // También deportes organizados
            partidoRepository.findByOrganizador(usuario).stream()
                    .map(p -> p.getDeporte().getNombre().toUpperCase())
                    .forEach(deportes::add);
            
            System.out.println("   🏀 Deportes totales (jugados + organizados): " + deportes);
            return deportes;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo deportes jugados: " + e.getMessage());
            e.printStackTrace();
            return Set.of();
        }
    }

    // Cuenta la cantidad de partidos jugados de otros deportes (no el exacto)
    private int contarPartidosOtrosDeportes(Usuario usuario, String deportePartido) {
        try {
            return (int) partidoRepository.findPartidosConJugador(usuario).stream()
                .filter(p -> !p.getDeporte().getNombre().equalsIgnoreCase(deportePartido))
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String getNombre() {
        return "POR_HISTORIAL";
    }
}