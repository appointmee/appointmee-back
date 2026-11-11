package com.gg.turnlook.features.turno;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TurnoScheduler {

    private final TurnoService turnoService;

    public TurnoScheduler(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @Scheduled(fixedRate = 60000)
    public void cancelarTurnosPendientesVencidos() {
        turnoService.cancelarTurnosPendientesVencidos();
    }
}
