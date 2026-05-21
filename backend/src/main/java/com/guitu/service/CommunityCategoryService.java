package com.guitu.service;

import com.guitu.domain.CommunityCategory;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CommunityCategoryService {
    private final CommunityCategoryRepository categoryRepository;

    public CommunityCategoryService(CommunityCategoryRepository categoryRepository) { this.categoryRepository = categoryRepository; }

    public List<CategoryResponse> listEnabled() {
        return categoryRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .map(this::toResponse).toList();
    }

    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(SaveCategoryRequest req) {
        if (categoryRepository.existsByCode(req.code()))
            throw new BusinessException("版块代码已存在");
        CommunityCategory c = new CommunityCategory();
        apply(c, req);
        return toResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, SaveCategoryRequest req) {
        CommunityCategory c = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("版块不存在"));
        if (!c.getCode().equals(req.code()) && categoryRepository.existsByCode(req.code()))
            throw new BusinessException("版块代码已存在");
        apply(c, req);
        return toResponse(categoryRepository.save(c));
    }

    public CommunityCategory getEntity(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("版块不存在"));
    }

    private void apply(CommunityCategory c, SaveCategoryRequest req) {
        c.setCode(req.code()); c.setName(req.name()); c.setNameEn(req.nameEn());
        c.setDescription(req.description()); c.setIcon(req.icon());
        c.setSortOrder(req.sortOrder()); c.setEnabled(req.enabled());
    }

    private CategoryResponse toResponse(CommunityCategory c) {
        return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getNameEn(),
            c.getDescription(), c.getIcon(), c.getSortOrder(), c.isEnabled(), c.getPostCount());
    }
}
