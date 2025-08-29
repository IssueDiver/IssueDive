package com.example.issueDive.dto;


import com.example.issueDive.entity.Issue;
import com.example.issueDive.entity.Label;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class IssueLabelsResponse {
    private Long id;
    private List<LabelSummary> labels;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class LabelSummary {
        private Long id;
        private String name;
    }

    public static IssueLabelsResponse of(Long issueId, List<Label> labels) {
        List<LabelSummary> summaries = new ArrayList<>();
        for (Label label : labels) {
            summaries.add(LabelSummary.builder()
                    .id(label.getId())
                    .name(label.getName())
                    .build());
        }

        return IssueLabelsResponse.builder()
                .id(issueId)
                .labels(summaries)
                .build();
    }
}
