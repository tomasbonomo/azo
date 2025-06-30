package com.uade.tpo.deportes.service;

import com.uade.tpo.deportes.dto.ActualizarDeporteRequest;
import com.uade.tpo.deportes.dto.CrearDeporteRequest;
import com.uade.tpo.deportes.dto.DeporteResponse;
import com.uade.tpo.deportes.entity.Deporte;
import com.uade.tpo.deportes.exceptions.DeporteYaExisteException;
import com.uade.tpo.deportes.exceptions.DeporteNoEncontradoException;
import com.uade.tpo.deportes.repository.DeporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeporteService {

    private final DeporteRepository deporteRepository;

    public List<DeporteResponse> obtenerTodosLosDeportes() {
        return deporteRepository.findAll().stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public List<DeporteResponse> obtenerDeportesActivos() {
        return deporteRepository.findByActivoTrue().stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public DeporteResponse obtenerDeportePorId(Long id) {
        Deporte deporte = deporteRepository.findById(id)
                .orElseThrow(() -> new DeporteNoEncontradoException("Deporte no encontrado con ID: " + id));
        return mapearAResponse(deporte);
    }

    public DeporteResponse crearDeporte(CrearDeporteRequest request) {
        if (deporteRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new DeporteYaExisteException("Ya existe un deporte con el nombre: " + request.getNombre());
        }

        Deporte deporte = Deporte.builder()
                .nombre(request.getNombre())
                .jugadoresPorEquipo(request.getJugadoresPorEquipo())
                .reglasBasicas(request.getReglasBasicas())
                .activo(true)
                .build();

        if (!deporte.validarConfiguracion()) {
            throw new IllegalArgumentException("Configuración de deporte inválida");
        }

        Deporte deporteGuardado = deporteRepository.save(deporte);
        return mapearAResponse(deporteGuardado);
    }

    public DeporteResponse actualizarDeporte(Long id, ActualizarDeporteRequest request) {
        Deporte deporte = deporteRepository.findById(id)
                .orElseThrow(() -> new DeporteNoEncontradoException("Deporte no encontrado con ID: " + id));

        // Verificar si el nuevo nombre ya existe (si se está cambiando)
        if (request.getNombre() != null && !request.getNombre().equalsIgnoreCase(deporte.getNombre())) {
            if (deporteRepository.existsByNombreIgnoreCase(request.getNombre())) {
                throw new DeporteYaExisteException("Ya existe un deporte con el nombre: " + request.getNombre());
            }
        }

        // Actualizar campos
        if (request.getNombre() != null) {
            deporte.setNombre(request.getNombre());
        }
        if (request.getJugadoresPorEquipo() != null) {
            deporte.setJugadoresPorEquipo(request.getJugadoresPorEquipo());
        }
        if (request.getReglasBasicas() != null) {
            deporte.setReglasBasicas(request.getReglasBasicas());
        }
        if (request.getActivo() != null) {
            deporte.setActivo(request.getActivo());
        }

        if (!deporte.validarConfiguracion()) {
            throw new IllegalArgumentException("Configuración de deporte inválida");
        }

        Deporte deporteActualizado = deporteRepository.save(deporte);
        return mapearAResponse(deporteActualizado);
    }

    public void eliminarDeporte(Long id) {
        Deporte deporte = deporteRepository.findById(id)
                .orElseThrow(() -> new DeporteNoEncontradoException("Deporte no encontrado con ID: " + id));
        
        // Soft delete - marcar como inactivo
        deporte.setActivo(false);
        deporteRepository.save(deporte);
    }

    private DeporteResponse mapearAResponse(Deporte deporte) {
        return DeporteResponse.builder()
                .id(deporte.getId())
                .nombre(deporte.getNombre())
                .jugadoresPorEquipo(deporte.getJugadoresPorEquipo())
                .reglasBasicas(deporte.getReglasBasicas())
                .activo(deporte.getActivo())
                .build();
    }
} 