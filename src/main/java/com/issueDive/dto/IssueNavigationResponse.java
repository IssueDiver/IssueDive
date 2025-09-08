package com.issueDive.dto;

/**
 * 이슈 상세 페이지에서 이전/다음 이슈 탐색을 위한 응답 DTO
 * @param previousIssueId 이전 이슈 ID (없으면 null)
 * @param nextIssueId 다음 이슈 ID (없으면 null)
 */
public record IssueNavigationResponse(
        Long previousIssueId,
        Long nextIssueId
) {
}