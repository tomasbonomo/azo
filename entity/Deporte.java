package com.uade.tpo.deportes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deportes")
public class Deporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer jugadoresPorEquipo;

    @Column(columnDefinition = "TEXT")
    private String reglasBasicas;

    @Column(nullable = false)
    private Boolean activo = true;

    public boolean validarConfiguracion() {
        return nombre != null && 
               !nombre.trim().isEmpty() && 
               jugadoresPorEquipo != null && 
               jugadoresPorEquipo > 0;
    }
}
