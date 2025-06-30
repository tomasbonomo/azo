package com.uade.tpo.deportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeporteResponse {
    private Long id;
    private String nombre;
    private Integer jugadoresPorEquipo;
    private String reglasBasicas;
    private Boolean activo;
}