package com.uade.tpo.deportes.dto;

import com.uade.tpo.deportes.enums.NivelJuego;
import com.uade.tpo.deportes.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombreUsuario;
    private String email;
    private String deporteFavorito;
    private NivelJuego nivelJuego;
    private Role role;
    private boolean activo;
    private LocalDateTime createdAt;
    private UbicacionResponse ubicacion;
    private String pushToken;
}