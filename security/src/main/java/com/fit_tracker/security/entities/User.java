package com.fit_tracker.security.entities;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;


    @Column(unique = true, nullable = false)
    private String userName;


    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public User(String userName, String password, Role role) {
        this.userName = userName;
        this.password = password;
        this.role = role;
    }
}
