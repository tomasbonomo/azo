package com.uade.tpo.deportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrearPartidoRequest {
    private Long deporteId;
    private int cantidadJugadoresRequeridos;
    private int duracion; // en minutos
    private UbicacionRequest ubicacion;
    private LocalDateTime horario;
    private String estrategiaEmparejamiento; // "POR_NIVEL", "POR_CERCANIA", "POR_HISTORIAL"
}
