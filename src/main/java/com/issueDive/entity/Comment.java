package com.example.issueDive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "issue_id", nullable = false)
        private Issue issue;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Lob
        @Column(nullable = false)
        private String description;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "parent_id")
        private Comment parent;

        @Builder.Default
        @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Comment> children = new ArrayList<>();

        public void changeDescription(String content) { this.description = description; }
}
