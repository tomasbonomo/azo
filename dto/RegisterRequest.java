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
public class RegisterRequest {
    private String nombreUsuario;
    private String email;
    private String contrasena;
    private Long deporteFavoritoId; // Opcional - ID del deporte favorito
    private NivelJuego nivelJuego; // Opcional
    private UbicacionRequest ubicacion;
}
