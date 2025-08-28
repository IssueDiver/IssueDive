package com.issueDive.dto;

import com.issueDive.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private List<CommentResponse> children;

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

        // 1) 모든 엔티티 → DTO 변환 & 인덱싱
        for (Comment c : comments) {
            map.put(c.getId(), from(c));
        }

        // 2) 부모-자식 연결
        for (Comment c : comments) {
            Long parentId = (c.getParent() != null) ? c.getParent().getId() : null;
            CommentResponse dto = map.get(c.getId());

            if (parentId == null) {
                roots.add(dto);
            } else {
                CommentResponse parentDto = map.get(parentId);
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    // 데이터 정합성 이슈 방어: 부모 DTO가 없으면 루트로 승격
                    roots.add(dto);
                }
            }
        }
        return roots;
    }


}
