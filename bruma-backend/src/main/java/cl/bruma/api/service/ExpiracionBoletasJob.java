package cl.bruma.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Libera el stock de órdenes que quedaron PENDIENTES (el cliente nunca terminó de pagar).
 * Solo se activa si INTERNAL_KEY está configurada: sin la integración con Webpay, Spring no se entera de los pagos
 * aprobados y anularía por error órdenes que sí se pagaron.
 */
@Component
@ConditionalOnExpression("!'${app.internal-key:}'.isEmpty()")
public class ExpiracionBoletasJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionBoletasJob.class);

    private final BoletaService boletas;
    private final int minutos;

    public ExpiracionBoletasJob(BoletaService boletas, @Value("${app.boletas.expiran-minutos:30}") int minutos) {
        this.boletas = boletas;
        this.minutos = minutos;
    }

    @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L) // cada 5 minutos
    public void anularVencidas() {
        int n = boletas.expirarPendientes(minutos);
        if (n > 0) {
            log.info("{} orden(es) pendientes expiraron y se repuso su stock", n);
        }
    }
}
