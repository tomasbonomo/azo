package com.uade.tpo.deportes.dto;

import com.uade.tpo.deportes.enums.NivelJuego;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CriteriosBusquedaUsuario {
    private Long deporteFavoritoId; // ID del deporte favorito
    private NivelJuego nivelJuego;
    private String zona;
    private boolean soloActivos = true;
}
