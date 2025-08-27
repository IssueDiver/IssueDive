package com.issueDive.service;

import com.issueDive.dto.CreateLabelRequest;
import com.issueDive.dto.LabelResponse;
import com.issueDive.entity.Label;
import com.issueDive.repository.LabelRepository;
import com.issueDive.util.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository _labelRepository;

    //라벨 생성
    @Transactional
    public ServiceResult<LabelResponse> createLabel(CreateLabelRequest _request) {
        if (_labelRepository.existsByNameIgnoreCase(_request.getName())) {
            return ServiceResult.error("DUPLICATED", "Label with name " + _request.getName() + " already exists");
        }
        Label _savedLabel = _labelRepository.save(
                Label.builder()
                        .name(_request.getName())
                        .color(_request.getColor())
                        .description(_request.getDescription())
                        .build()
        );

        LabelResponse _response = LabelResponse.from(_savedLabel);

        return ServiceResult.success(_response);
    }

    //라벨 목록 조회
    @Transactional(readOnly = true)
    public ServiceResult<List<LabelResponse>> getLabels() {
        List<Label> _savedLabels = _labelRepository.findAll();
        List<LabelResponse> _response = _savedLabels.stream()
                .map(LabelResponse::from)
                .toList();

        return ServiceResult.success(_response);
    }

    //단일 라벨 조회
    @Transactional(readOnly = true)
    public ServiceResult<LabelResponse> getLabel(Long _id) {
        Label _label = _labelRepository.findByIdIgnoreCase(_id);
        if (_label == null) {
            return ServiceResult.error("NOT_FOUND", "Label with name " + _id + " not found");
        }

        LabelResponse _response = LabelResponse.from(_label);

        return ServiceResult.success(_response);
    }
}
