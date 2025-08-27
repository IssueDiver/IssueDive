package com.example.issueDive.service;

import com.example.issueDive.dto.CreateLabelRequest;
import com.example.issueDive.dto.LabelResponse;
import com.example.issueDive.entity.Label;
import com.example.issueDive.exception.ErrorCode;
import com.example.issueDive.exception.LabelNotFoundException;
import com.example.issueDive.exception.ValidationException;
import com.example.issueDive.repository.LabelRepository;
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
}
