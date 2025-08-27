package com.issueDive.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users") // user는 예약어라서 safe name

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    @Email
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "created_at",
            updatable = false,
            insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            insertable = false,
            updatable = false)
    private LocalDateTime updatedAt;

    public User(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
