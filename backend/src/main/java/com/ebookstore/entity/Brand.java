package com.ebookstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "brands")
@Getter @Setter @NoArgsConstructor
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
}
