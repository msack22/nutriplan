package com.nutriplan.recipe.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Categoría del recetario (Desayunos y meriendas, Panes, Postres...). */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 16)
    private String emoji;

    @Column(name = "sort_order")
    private int sortOrder;

    /** JPA necesita un constructor sin argumentos (puede ser protected). */
    protected Category() {
    }

    public Category(String slug, String name, String emoji, int sortOrder) {
        this.slug = slug;
        this.name = name;
        this.emoji = emoji;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
