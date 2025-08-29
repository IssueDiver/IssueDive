package com.example.issueDive.service;

import com.example.issueDive.dto.IssueLabelsResponse;
import com.example.issueDive.dto.LabelResponse;
import com.example.issueDive.entity.Issue;
import com.example.issueDive.entity.IssueLabel;
import com.example.issueDive.entity.IssueLabelId;
import com.example.issueDive.entity.Label;
import com.example.issueDive.exception.IssueLabelNotFoundException;
import com.example.issueDive.exception.LabelNotFoundException;
import com.example.issueDive.exception.NotFoundException;
import com.example.issueDive.repository.IssueLabelRepository;
import com.example.issueDive.repository.IssueRepository;
import com.example.issueDive.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IssueLabelService {

    private final IssueRepository issueRepository;
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;

    @Transactional
    public IssueLabelsResponse addLabelsToIssue(Long issueId, List<Long> labelIds){
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));

        List<Label> labels = labelRepository.findAllById(labelIds);


        Set<Long> foundIds = new HashSet<>();
        for (Label label : labels) {
            foundIds.add(label.getId());
        }

        Long missingId = null;
        for (Long id : labelIds) {
            if (!foundIds.contains(id)) {
                missingId = id;
                break;
            }
        }
        if (missingId != null) {
            throw new LabelNotFoundException("Label not found: id=" + missingId);
        }


        for (Label label : labels) {
            boolean alreadyExists = issueLabelRepository.existsByIssueAndLabel(issue, label);
            if (!alreadyExists) {
                IssueLabel issueLabel = IssueLabel.builder()
                        .id(new IssueLabelId(issue.getId(), label.getId()))
                        .issue(issue)
                        .label(label)
                        .build();
                issueLabelRepository.save(issueLabel);
            }
        }

        List<Label> currentLabels = getLabelsOfIssue(issue);

        return IssueLabelsResponse.of(issueId, currentLabels);
    }

    @Transactional(readOnly = true)
    public List<Label> getLabelsOfIssue(Issue issue){
        List<IssueLabel> mappings = issueLabelRepository.findAllByIssue(issue);
        List<Label> currentLabels = new ArrayList<>();
        for (IssueLabel issueLabel : mappings) {
            currentLabels.add(issueLabel.getLabel());
        }
        return currentLabels;
    }

    @Transactional
    public LabelResponse deleteLabelFromIssue(Long issueId, Long labelId){
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found"));
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new NotFoundException("Label not found"));

        boolean exists = issueLabelRepository.existsByIssueAndLabel(issue, label);

        if (!exists){
            throw new IssueLabelNotFoundException("Label " + labelId + "is not attached to Issue " + issueId);
        }

        issueLabelRepository.deleteByIssueAndLabel(issue, label);

        return LabelResponse.from(label);
    }

}
