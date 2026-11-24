package com.nutriplan.pricing.domain.port;

import com.nutriplan.common.events.PricesRefreshedEvent;

/** Puerto: avisar a otros servicios que hay precios nuevos. */
public interface RefreshEventPublisher {

    void publish(PricesRefreshedEvent event);
}
