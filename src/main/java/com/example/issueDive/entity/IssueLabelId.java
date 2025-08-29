package com.example.issueDive.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssueLabelId implements Serializable {
    private Long issueId;
    private Long labelId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueLabelId that)) return false;
        return Objects.equals(issueId, that.issueId) &&
                Objects.equals(labelId, that.labelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, labelId);
    }
}
