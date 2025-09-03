package com.issueDive.dto;

import com.issueDive.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private Long id;
    private Long issueId;
    private Long userId;
    private String author;
    private String description;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CommentResponse> children = new ArrayList<>();

    // 방어적 추가 헬퍼
    public void addChild(CommentResponse child) {
        if (children == null) children = new ArrayList<>();
        children.add(child);
    }

    // 널-세이프 getter
    public List<CommentResponse> getChildren() {
        if (children == null) children = new ArrayList<>();
        return children;
    }

    public static CommentResponse from(Comment comment){
        return CommentResponse.builder()
                .id(comment.getId())
                .issueId(comment.getIssue().getId())
                .userId(comment.getUser().getId())
                .author(comment.getUser().getUsername())
                .description(comment.getDescription())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static List<CommentResponse> fromAllToTree(List<Comment> comments){
        Map<Long, CommentResponse> map = new LinkedHashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        // 1) 엔티티 → DTO 변환 & 인덱싱
        for (Comment c : comments) {
            map.put(c.getId(), from(c));
        }

        // 2) 부모-자식 연결
        for (Comment c : comments) {
            Long pId = (c.getParent() != null) ? c.getParent().getId() : null;
            CommentResponse dto = map.get(c.getId());

            if (pId == null) {
                roots.add(dto);
            } else {
                CommentResponse parent = map.get(pId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    // 부모 누락 방어: 루트에 승격(정책에 맞게 조정 가능)
                    roots.add(dto);
                }
            }
        }
        return roots;
    }
}
