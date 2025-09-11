package com.issueDive.repository;

import com.issueDive.dto.LabelResponse;
import com.issueDive.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label,Long> {
    boolean existsByNameIgnoreCase(String name);

    // 모든 라벨에 대해, 'OPEN' 상태인 이슈의 개수를 LEFT JOIN과 GROUP BY를 사용해 한 번에 조회합니다.
    @Query("SELECT new com.issueDive.dto.LabelResponse(l.id, l.name, l.color, l.description, COUNT(il)) " +
            "FROM Label l LEFT JOIN IssueLabel il ON l.id = il.label.id AND il.issue.status = 'OPEN' " +
            "GROUP BY l.id, l.name, l.color, l.description")
    List<LabelResponse> findAllWithOpenIssueCount();
}
