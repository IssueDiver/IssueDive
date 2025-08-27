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

        // 어떤 이슈에 달린 댓글인지
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "issue_id", nullable = false)
        private Issue issue;

        // 댓글 작성자
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        // 댓글 내용
        @Lob
        @Column(nullable = false)
        private String description;

        // 부모 댓글 (null이면 일반 댓글, 있으면 대댓글)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "parent_id")
        private Comment parent;

        // 자식 댓글들 (대댓글 리스트)
        @Builder.Default
        @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Comment> children = new ArrayList<>();

}
