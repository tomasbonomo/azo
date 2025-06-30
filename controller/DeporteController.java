package com.uade.tpo.deportes.controller;

import com.uade.tpo.deportes.dto.ActualizarDeporteRequest;
import com.uade.tpo.deportes.dto.CrearDeporteRequest;
import com.uade.tpo.deportes.dto.DeporteResponse;
import com.uade.tpo.deportes.service.DeporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/deportes")
@RequiredArgsConstructor
public class DeporteController {

    private final DeporteService deporteService;

    @GetMapping
    public ResponseEntity<List<DeporteResponse>> obtenerDeportes() {
        List<DeporteResponse> deportes = deporteService.obtenerTodosLosDeportes();
        return ResponseEntity.ok(deportes);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<DeporteResponse>> obtenerDeportesActivos() {
        List<DeporteResponse> deportes = deporteService.obtenerDeportesActivos();
        return ResponseEntity.ok(deportes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeporteResponse> obtenerDeporte(@PathVariable Long id) {
        try {
            DeporteResponse deporte = deporteService.obtenerDeportePorId(id);
            return ResponseEntity.ok(deporte);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<DeporteResponse> crearDeporte(@RequestBody CrearDeporteRequest request) {
        try {
            DeporteResponse deporte = deporteService.crearDeporte(request);
            return ResponseEntity.ok(deporte);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeporteResponse> actualizarDeporte(
            @PathVariable Long id, 
            @RequestBody ActualizarDeporteRequest request) {
        try {
            DeporteResponse deporte = deporteService.actualizarDeporte(id, request);
            return ResponseEntity.ok(deporte);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDeporte(@PathVariable Long id) {
        try {
            deporteService.eliminarDeporte(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}