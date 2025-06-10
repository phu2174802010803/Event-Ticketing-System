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
            throw new TemplateAlreadyExistsException(
                    "Template với tên '" + requestDto.getName() + "' đã tồn tại.");
        }

        MapTemplate template = new MapTemplate();
        template.setName(requestDto.getName());
        template.setDescription(requestDto.getDescription());
        template.setAreaCount(requestDto.getAreaCount());
        template.setMapWidth(requestDto.getMapWidth());
        template.setMapHeight(requestDto.getMapHeight());

        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            List<TemplateArea> areas = requestDto.getAreas().stream()
                    .map(areaDto -> {
                        TemplateArea area = new TemplateArea();
                        area.setName(areaDto.getName());
                        area.setX(areaDto.getX());
                        area.setY(areaDto.getY());
                        area.setWidth(areaDto.getWidth());
                        area.setHeight(areaDto.getHeight());
                        area.setVertices(areaDto.getVertices());
                        area.setZone(areaDto.getZone());
                        area.setFillColor(areaDto.getFillColor());
                        area.setStage(areaDto.isStage());
                        area.setMapTemplate(template);
                        return area;
                    })
                    .collect(Collectors.toList());
            template.setAreas(areas);
        }

        MapTemplate savedTemplate = mapTemplateRepository.save(template);

        List<TemplateAreaResponseDto> areaDtos = savedTemplate.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName(),
                        area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                        area.getVertices(), area.getZone(), area.getFillColor(), area.isStage()))
                .collect(Collectors.toList());

        return new MapTemplateResponseDto(
                savedTemplate.getTemplateId(),
                savedTemplate.getName(),
                savedTemplate.getDescription(),
                savedTemplate.getAreaCount(),
                savedTemplate.getMapWidth(),
                savedTemplate.getMapHeight(),
                areaDtos,
                "Tạo template map thành công");
    }

    // Thêm khu vực mới
    @Transactional
    public TemplateAreaResponseDto createArea(Integer templateId, TemplateAreaRequestDto requestDto) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));

        TemplateArea area = new TemplateArea();
        area.setName(requestDto.getName());
        area.setX(requestDto.getX());
        area.setY(requestDto.getY());
        area.setWidth(requestDto.getWidth());
        area.setHeight(requestDto.getHeight());
        area.setVertices(requestDto.getVertices());
        area.setZone(requestDto.getZone());
        area.setFillColor(requestDto.getFillColor());
        area.setMapTemplate(template);

        TemplateArea savedArea = templateAreaRepository.save(area);
        template.setAreaCount(template.getAreas().size() + 1); // Cập nhật area_count
        mapTemplateRepository.save(template);

        return new TemplateAreaResponseDto(savedArea.getTemplateAreaId(), savedArea.getName(),
                savedArea.getX(), savedArea.getY(), savedArea.getWidth(), savedArea.getHeight(),
                savedArea.getVertices(), savedArea.getZone(), savedArea.getFillColor(), savedArea.isStage());
    }

    public List<MapTemplateResponseDto> getAllTemplates() {
        List<MapTemplate> templates = mapTemplateRepository.findAll();
        return templates.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public MapTemplateResponseDto getTemplateById(Integer templateId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));
        return convertToResponseDto(template);
    }

    // Xem danh sách khu vực của mẫu map
    @Transactional(readOnly = true)
    public List<TemplateAreaResponseDto> getAreasByTemplate(Integer templateId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));
        return template.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName(),
                        area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                        area.getVertices(), area.getZone(), area.getFillColor(), area.isStage()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MapTemplateResponseDto updateTemplate(Integer templateId, MapTemplateRequestDto requestDto) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));

        if (!template.getName().equals(requestDto.getName())
                && mapTemplateRepository.existsByName(requestDto.getName())) {
            throw new TemplateAlreadyExistsException(
                    "Template với tên '" + requestDto.getName() + "' đã tồn tại.");
        }

        template.setName(requestDto.getName());
        template.setDescription(requestDto.getDescription());
        template.setMapWidth(requestDto.getMapWidth());
        template.setMapHeight(requestDto.getMapHeight());

        // // Xóa các khu vực cũ
        // template.getAreas().clear();
        //
        // // Thêm các khu vực mới
        // if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
        // List<TemplateArea> areas = requestDto.getAreas().stream()
        // .map(areaDto -> {
        // TemplateArea area = new TemplateArea();
        // area.setName(areaDto.getName());
        // area.setX(areaDto.getX());
        // area.setY(areaDto.getY());
        // area.setWidth(areaDto.getWidth());
        // area.setHeight(areaDto.getHeight());
        // area.setVertices(areaDto.getVertices());
        // area.setZone(areaDto.getZone());
        // area.setFillColor(areaDto.getFillColor());
        // area.setMapTemplate(template);
        // return area;
        // })
        // .collect(Collectors.toList());
        // template.getAreas().addAll(areas);
        // }

        MapTemplate updatedTemplate = mapTemplateRepository.save(template);

        return convertToResponseDto(updatedTemplate);
    }

    // Cập nhật khu vực
    @Transactional
    public TemplateAreaResponseDto updateArea(Integer templateId, Integer areaId,
                                              TemplateAreaRequestDto requestDto) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));
        TemplateArea area = templateAreaRepository.findById(areaId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Khu vực với ID " + areaId + " không tồn tại."));

        if (!area.getMapTemplate().getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("Khu vực không thuộc mẫu map này.");
        }

        area.setName(requestDto.getName());
        area.setX(requestDto.getX());
        area.setY(requestDto.getY());
        area.setWidth(requestDto.getWidth());
        area.setHeight(requestDto.getHeight());
        area.setVertices(requestDto.getVertices());
        area.setZone(requestDto.getZone());
        area.setFillColor(requestDto.getFillColor());

        TemplateArea updatedArea = templateAreaRepository.save(area);
        return new TemplateAreaResponseDto(updatedArea.getTemplateAreaId(), updatedArea.getName(),
                updatedArea.getX(), updatedArea.getY(), updatedArea.getWidth(), updatedArea.getHeight(),
                updatedArea.getVertices(), updatedArea.getZone(), updatedArea.getFillColor(), updatedArea.isStage());
    }

    @Transactional
    public void deleteTemplate(Integer templateId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));
        mapTemplateRepository.delete(template);
    }

    // Xóa khu vực
    @Transactional
    public void deleteArea(Integer templateId, Integer areaId) {
        MapTemplate template = mapTemplateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Template với ID " + templateId + " không tồn tại."));
        TemplateArea area = templateAreaRepository.findById(areaId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Khu vực với ID " + areaId + " không tồn tại."));

        if (!area.getMapTemplate().getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("Khu vực không thuộc mẫu map này.");
        }

        // Xóa area khỏi collection trước
        template.getAreas().remove(area);

        // Cập nhật area_count dựa trên collection đã được update
        template.setAreaCount(template.getAreas().size());

        // Xóa area khỏi database
        templateAreaRepository.delete(area);

        // Lưu template với area_count đã cập nhật
        mapTemplateRepository.save(template);
    }

    private MapTemplateResponseDto convertToResponseDto(MapTemplate template) {
        List<TemplateAreaResponseDto> areaDtos = template.getAreas().stream()
                .map(area -> new TemplateAreaResponseDto(area.getTemplateAreaId(), area.getName(),
                        area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                        area.getVertices(), area.getZone(), area.getFillColor(), area.isStage()))
                .collect(Collectors.toList());
        return new MapTemplateResponseDto(
                template.getTemplateId(),
                template.getName(),
                template.getDescription(),
                template.getAreaCount(),
                template.getMapWidth(),
                template.getMapHeight(),
                areaDtos, "Cập nhật template map thành công"
                // template.getAreas().isEmpty() ? null : "Cập nhật template map thành công"
        );
    }
}