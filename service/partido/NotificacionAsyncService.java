package com.uade.tpo.deportes.service.partido;

import com.uade.tpo.deportes.entity.Partido;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificacionAsyncService {
    @Async
    public void notificarCreacionPartido(Partido partido) {
        System.out.println("🚀 === NOTIFICACION ASYNC SERVICE ===");
        System.out.println("   Partido ID: " + partido.getId());
        System.out.println("   Deporte: " + partido.getDeporte().getNombre());
        System.out.println("   Llamando a notificarObservers()...");
        
        partido.notificarObservers();
        
        System.out.println("   ✅ notificarObservers() completado");
    }
    
    // ✅ TEST: Método síncrono para verificar si el problema es @Async
    public void notificarCreacionPartidoSincrono(Partido partido) {
        System.out.println("🚀 === NOTIFICACION SINCRONA SERVICE ===");
        System.out.println("   Partido ID: " + partido.getId());
        System.out.println("   Deporte: " + partido.getDeporte().getNombre());
        System.out.println("   Llamando a notificarObservers()...");
        
        partido.notificarObservers();
        
        System.out.println("   ✅ notificarObservers() completado");
    }
} 