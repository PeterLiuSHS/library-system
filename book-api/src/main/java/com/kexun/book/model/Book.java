package com.kexun.book.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {
    // ====attributes====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String isbn;

    private Integer publishedYear;

    private LocalDateTime createdAt;

    // ====no args constructor: for JPA====
    public Book() {
    }

    // ====getter & setter====
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    public Integer getPublishedYear() {
        return publishedYear;
    }
    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
