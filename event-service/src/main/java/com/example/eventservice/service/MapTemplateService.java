package com.example.eventservice.service;

import com.example.eventservice.dto.MapTemplateRequestDto;
import com.example.eventservice.dto.MapTemplateResponseDto;
import com.example.eventservice.dto.TemplateAreaRequestDto;
import com.example.eventservice.dto.TemplateAreaResponseDto;
import com.example.eventservice.exception.TemplateAlreadyExistsException;
import com.example.eventservice.exception.TemplateNotFoundException;
import com.example.eventservice.model.MapTemplate;
import com.example.eventservice.model.TemplateArea;
import com.example.eventservice.repository.MapTemplateRepository;
import com.example.eventservice.repository.TemplateAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapTemplateService {

    @Autowired
    private MapTemplateRepository mapTemplateRepository;

    @Autowired
    private TemplateAreaRepository templateAreaRepository;

    @Transactional
    public MapTemplateResponseDto createTemplate(MapTemplateRequestDto requestDto) {
        if (mapTemplateRepository.existsByName(requestDto.getName())) {
            throw new TemplateAlreadyExistsException("Template với tên '" + requestDto.getName() + "' đã tồn tại.");
        }

            MapTemplate template = new MapTemplate();
            template.setName(requestDto.getName());
            template.setDescription(requestDto.getDescription());
            template.setAreaCount(requestDto.getAreaCount());

            if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
                List<TemplateArea> areas = requestDto.getAreas().stream()
                        .map(areaDto -> {
                            TemplateArea area = new TemplateArea();
                            area.setName(areaDto.getName());
                            area.setMapTemplate(template); // Thiết lập mối quan hệ với template, để JPA hiểu rằng mỗi khu vực này là con của template cha đó
                            return area;
                        })
                        .collect(Collectors.toList());
                template.setAreas(areas);
            }

        MapTemplate savedTemplate = mapTemplateRepository.save(template);

        List<TemplateAreaResponseDto> areaDtos = savedTemplate.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName()))
                .collect(Collectors.toList());

        return new MapTemplateResponseDto(
                savedTemplate.getTemplateId(),
                savedTemplate.getName(),
                savedTemplate.getDescription(),
                savedTemplate.getAreaCount(),
                areaDtos,
                "Tạo template map thành công"
        );
    }

    public List<MapTemplateResponseDto> getAllTemplates() {
        List<MapTemplate> templates = mapTemplateRepository.findAll();
        return templates.stream()
                .map(template -> {
                    List<TemplateAreaResponseDto> areaDtos = template.getAreas().stream()
                            .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName()))
                            .collect(Collectors.toList());
                    return new MapTemplateResponseDto(
                            template.getTemplateId(),
                            template.getName(),
                            template.getDescription(),
                            template.getAreaCount(),
                            areaDtos,
                            null
                    );
                })
                .collect(Collectors.toList());
    }

    public MapTemplateResponseDto getTemplateById(Integer templateId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template với ID " + templateId + " không tồn tại."));
        List<TemplateAreaResponseDto> areaDtos = template.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName()))
                .collect(Collectors.toList());
        return new MapTemplateResponseDto(
                template.getTemplateId(),
                template.getName(),
                template.getDescription(),
                template.getAreaCount(),
                areaDtos,
                null
        );
    }

    @Transactional
    public MapTemplateResponseDto updateTemplate(Integer templateId, MapTemplateRequestDto requestDto) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template với ID " + templateId + " không tồn tại."));

        if (!template.getName().equals(requestDto.getName()) && mapTemplateRepository.existsByName(requestDto.getName())) {
            throw new TemplateAlreadyExistsException("Template với tên '" + requestDto.getName() + "' đã tồn tại.");
        }

        template.setName(requestDto.getName());
        template.setDescription(requestDto.getDescription());
        template.setAreaCount(requestDto.getAreaCount());

        // Xóa các khu vực cũ
        template.getAreas().clear();

        // Thêm các khu vực mới
        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            List<TemplateArea> areas = requestDto.getAreas().stream()
                    .map(areaDto -> {
                        TemplateArea area = new TemplateArea();
                        area.setName(areaDto.getName());
                        area.setMapTemplate(template);
                        return area;
                    })
                    .collect(Collectors.toList());
            template.getAreas().addAll(areas);
        }

        MapTemplate updatedTemplate = mapTemplateRepository.save(template);

        List<TemplateAreaResponseDto> areaDtos = updatedTemplate.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName()))
                .collect(Collectors.toList());

        return new MapTemplateResponseDto(
                updatedTemplate.getTemplateId(),
                updatedTemplate.getName(),
                updatedTemplate.getDescription(),
                updatedTemplate.getAreaCount(),
                areaDtos,
                "Cập nhật template map thành công"
        );
    }

    @Transactional
    public void deleteTemplate(Integer templateId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template với ID " + templateId + " không tồn tại."));
        mapTemplateRepository.delete(template);
    }
}