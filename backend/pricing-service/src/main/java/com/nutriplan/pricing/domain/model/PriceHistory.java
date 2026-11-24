package com.nutriplan.pricing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Precio de un ingrediente en una cadena en una fecha: permite graficar la inflación semanal. */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String chain;

    @Column(nullable = false, length = 200)
    private String ingredient;

    @Column(name = "price_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double price;

    @Column(name = "product_name", nullable = false)
    private String productName;

    protected PriceHistory() {
    }

    public PriceHistory(String chain, String ingredient, LocalDate date, double price, String productName) {
        this.chain = chain;
        this.ingredient = ingredient;
        this.date = date;
        this.price = price;
        this.productName = productName;
    }

    public String getChain() {
        return chain;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPrice() {
        return price;
    }

    public String getProductName() {
        return productName;
    }
}
