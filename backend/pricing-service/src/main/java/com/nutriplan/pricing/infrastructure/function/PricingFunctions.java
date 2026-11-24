package com.nutriplan.pricing.infrastructure.function;

import com.nutriplan.pricing.application.dto.EstimateRequest;
import com.nutriplan.pricing.application.dto.PriceEstimate;
import com.nutriplan.pricing.application.dto.RefreshReport;
import com.nutriplan.pricing.application.usecase.PriceEstimationService;
import com.nutriplan.pricing.application.usecase.PriceRefreshService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Serverless con Spring Cloud Function</b>: la lógica se expone como funciones Java estándar
 * ({@link Supplier}, {@link Function}). El MISMO código puede correr:
 * <ul>
 *   <li>como microservicio normal (los invoca el scheduler o el controller), o</li>
 *   <li>como AWS Lambda: el handler {@code FunctionInvoker} de spring-cloud-function-adapter-aws
 *       invoca la función indicada en {@code spring.cloud.function.definition}
 *       (ver infra/aws/template.yaml y docs/09-serverless.md). EventBridge Scheduler dispara
 *       {@code refreshPrices} todos los lunes.</li>
 * </ul>
 */
@Configuration
public class PricingFunctions {

    @Bean
    public Supplier<RefreshReport> refreshPrices(PriceRefreshService service) {
        return () -> service.refreshAll(null);
    }

    @Bean
    public Function<EstimateRequest, PriceEstimate> estimatePrices(PriceEstimationService service) {
        return service::estimate;
    }
}
