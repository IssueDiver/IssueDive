package com.issueDive.service;

import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.dto.UpdateLabelRequest;
import com.issueDive.entity.IssueStatus;
import com.issueDive.entity.Label;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.LabelNotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.LabelRepository;
import com.issueDive.repository.IssueLabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;

    //라벨 생성
    @Transactional
    public LabelResponse createLabel(CreateLabelRequest request) {
        if (labelRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException(ErrorCode.DuplicateLabel,
                    "Label with name " + request.getName() + " already exists");
        }
        Label savedLabel = labelRepository.save(
                Label.builder()
                        .name(request.getName())
                        .color(request.getColor())
                        .description(request.getDescription())
                        .build()
        );

        long issueOpenCount = issueLabelRepository.countByLabelAndIssue_Status(savedLabel, IssueStatus.OPEN);
        return LabelResponse.from(savedLabel,  issueOpenCount);
    }

    //라벨 목록 조회
    @Transactional(readOnly = true)
    public List<LabelResponse> getLabels() {
        return labelRepository.findAllWithOpenIssueCount();
    }

    //단일 라벨 조회
    @Transactional(readOnly = true)
    public LabelResponse getLabel(Long id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new LabelNotFoundException("Label not found: id=" + id));

        long issueOpenCount = issueLabelRepository.countByLabelAndIssue_Status(label, IssueStatus.OPEN);
        return LabelResponse.from(label,  issueOpenCount);
    }

    //라벨 수정
    @Transactional
    public LabelResponse updateLabel(Long id, UpdateLabelRequest request) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new LabelNotFoundException("Label not found: id=" + id));

        //이름 변경 시 중복 체크
        if (request.getName() != null && !request.getName().equalsIgnoreCase(label.getName())) {
            if (labelRepository.existsByNameIgnoreCase(request.getName())) {
                throw new ValidationException(ErrorCode.DuplicateLabel,
                        "Label with name " + request.getName() + " already exists");
            }
            label.setName(request.getName());
        }

        if (request.getColor() != null) {
            label.setColor(request.getColor());
        }

        if (request.getDescription() != null) {
            label.setDescription(request.getDescription());
        }

        Label updatedLabel = labelRepository.save(label);
        long issueOpenCount = issueLabelRepository.countByLabelAndIssue_Status(label, IssueStatus.OPEN);

        return LabelResponse.from(updatedLabel, issueOpenCount);
    }

    //라벨 삭제
    @Transactional
    public void deleteLabel(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new LabelNotFoundException("Label not found: id=" + id);
        }

        // 부모(label) 삭제 전에, 먼저 자식 테이블(issue_label)에서 해당 라벨을 사용하는 모든 연결 삭제
        // (존재 여부를 확인할 필요 없이 그냥 삭제함. 없으면 아무 일도 일어나지 않음.)
        issueLabelRepository.deleteByLabelId(id);

        labelRepository.deleteById(id);
    }

}
