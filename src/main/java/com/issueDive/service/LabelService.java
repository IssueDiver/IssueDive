package com.issueDive.service;

import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.dto.UpdateLabelRequest;
import com.issueDive.entity.Label;
import com.issueDive.exception.ErrorCode;
import com.issueDive.exception.LabelNotFoundException;
import com.issueDive.exception.ValidationException;
import com.issueDive.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

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

        return LabelResponse.from(savedLabel);
    }

    //라벨 목록 조회
    @Transactional(readOnly = true)
    public List<LabelResponse> getLabels() {

        return labelRepository.findAll().stream()
                .map(LabelResponse::from)
                .toList();
    }

    //단일 라벨 조회
    @Transactional(readOnly = true)
    public LabelResponse getLabel(Long id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new LabelNotFoundException("Label not found: id=" + id));

        return LabelResponse.from(label);
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

        return LabelResponse.from(updatedLabel);
    }

    //라벨 삭제
    @Transactional
    public void deleteLabel(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new LabelNotFoundException("Label not found: id=" + id);
        }
        labelRepository.deleteById(id);
    }
}
