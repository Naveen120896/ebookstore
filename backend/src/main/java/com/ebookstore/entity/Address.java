package com.ebookstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "addresses")
@Getter @Setter @NoArgsConstructor
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @Column(name = "is_default")
    private boolean defaultAddress;
}
