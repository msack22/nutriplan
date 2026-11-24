package com.nutriplan.pricing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Último precio conocido del mejor producto que matchea un ingrediente en una cadena.
 * Se actualiza con el job semanal; el historial queda en {@link PriceHistory}.
 */
@Entity
@Table(name = "product_price", uniqueConstraints = @UniqueConstraint(columnNames = {"chain", "ingredient"}))
public class ProductPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String chain;

    @Column(nullable = false, length = 200)
    private String ingredient;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(length = 120)
    private String brand;

    @Column(nullable = false)
    private double price;

    @Column(name = "package_size")
    private Double packageSize;

    /** g, ml o unit */
    @Column(name = "package_unit", length = 10)
    private String packageUnit;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ProductPrice() {
    }

    public ProductPrice(String chain, String ingredient) {
        this.chain = chain;
        this.ingredient = ingredient;
    }

    public void update(ProductOffer offer, PackageSize size, Instant now) {
        this.productName = offer.name();
        this.brand = offer.brand();
        this.price = offer.price();
        this.packageSize = size == null ? null : size.amount();
        this.packageUnit = size == null ? null : size.unit();
        this.productUrl = offer.url();
        this.fetchedAt = now;
    }

    public PackageSize packageSize() {
        return packageSize == null ? null : new PackageSize(packageSize, packageUnit);
    }

    public String getChain() {
        return chain;
    }

    public String getIngredient() {
        return ingredient;
    }

    public String getProductName() {
        return productName;
    }

    public String getBrand() {
        return brand;
    }

    public double getPrice() {
        return price;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
