package com.nutriplan.pricing.domain.port;

import com.nutriplan.pricing.domain.model.ProductOffer;
import java.util.List;

/**
 * Puerto: catálogo de un supermercado. Patrón <b>Adapter</b>: cada cadena (VTEX, una API
 * propia, el dataset SEPA del gobierno...) se adapta a esta interfaz común.
 */
public interface SupermarketCatalog {

    String chainId();

    String displayName();

    List<ProductOffer> search(String term, int limit);
}
