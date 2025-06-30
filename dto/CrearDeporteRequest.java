package com.uade.tpo.deportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearDeporteRequest {
    private String nombre;
    private Integer jugadoresPorEquipo;
    private String reglasBasicas;
} 