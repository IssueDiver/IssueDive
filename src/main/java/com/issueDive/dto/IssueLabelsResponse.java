package com.issueDive.dto;


import com.issueDive.entity.Label;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class IssueLabelsResponse implements Serializable {
    private Long id;
    private List<LabelSummary> labels;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class LabelSummary implements Serializable {
        private Long id;
        private String name;
        private String color;
    }

    public static IssueLabelsResponse of(Long issueId, List<Label> labels) {
        List<LabelSummary> summaries = new ArrayList<>();
        for (Label label : labels) {
            summaries.add(LabelSummary.builder()
                    .id(label.getId())
                    .name(label.getName())
                    .color(label.getColor())
                    .build());
        }

        return IssueLabelsResponse.builder()
                .id(issueId)
                .labels(summaries)
                .build();
    }
}
