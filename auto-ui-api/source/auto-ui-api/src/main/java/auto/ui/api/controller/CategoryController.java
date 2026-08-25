package auto.ui.api.controller;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.category.CategoryDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.category.CreateCategoryForm;
import auto.ui.api.form.category.UpdateCategoryForm;
import auto.ui.api.mapper.CategoryMapper;
import auto.ui.api.model.Category;
import auto.ui.api.model.criteria.CategoryCriteria;
import auto.ui.api.repository.CategoryRepository;
import auto.ui.api.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/v1/category")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class CategoryController extends ABasicController {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private FileService fileService;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CAT_V')")
    public ApiMessageDto<CategoryDto> get(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found category", ErrorCode.CATEGORY_ERROR_NOT_FOUND));
        return makeSuccessResponse(categoryMapper.fromEntityToCategoryDto(category), "Get category success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CAT_L')")
    public ApiMessageDto<ResponseListDto<List<CategoryDto>>> list(CategoryCriteria categoryCriteria,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(categoryCriteria.getCriteria(), pageable);
        ResponseListDto<List<CategoryDto>> responseListDto =
                makeResponseListDto(page, categoryMapper::fromEntityListToCategoryDtoList);
        return makeSuccessResponse(responseListDto, "Get list category success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<CategoryDto>>> autoComplete(CategoryCriteria categoryCriteria, Pageable pageable) {
        categoryCriteria.setStatus(AIConstant.STATUS_ACTIVE);
        Page<Category> page = categoryRepository.findAll(categoryCriteria.getCriteria(), pageable);
        return makeSuccessResponse(makeResponseListDto(page, categoryMapper::fromEntityListToCategoryDtoAutoComplete),
                "Get auto complete category success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CAT_C')")
    @Transactional
    public ApiMessageDto<CategoryDto> create(@Valid @RequestBody CreateCategoryForm createCategoryForm, BindingResult bindingResult) {
        if (categoryRepository.existsByName(createCategoryForm.getName())) {
            throw new BadRequestException("Category name already exist", ErrorCode.CATEGORY_ERROR_NAME_EXIST);
        }
        Category category = categoryMapper.fromCreateFormToEntity(createCategoryForm);
        categoryRepository.save(category);
        return makeSuccessResponse(categoryMapper.fromEntityToCategoryIdDto(category), "Create category success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CAT_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateCategoryForm updateCategoryForm, BindingResult bindingResult) {
        Category category = categoryRepository.findById(updateCategoryForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found category", ErrorCode.CATEGORY_ERROR_NOT_FOUND));
        if (!Objects.equals(updateCategoryForm.getName(), category.getName())
                && categoryRepository.existsByNameAndIdNot(updateCategoryForm.getName(), category.getId())) {
            throw new BadRequestException("Category name already exist", ErrorCode.CATEGORY_ERROR_NAME_EXIST);
        }
        String oldAvatar = category.getAvatar();
        if (StringUtils.isNotBlank(oldAvatar) && !Objects.equals(updateCategoryForm.getAvatar(), oldAvatar)) {
            fileService.deleteFile(oldAvatar);
        }
        categoryMapper.updateEntityFromForm(updateCategoryForm, category);
        categoryRepository.save(category);
        return makeSuccessResponse("Update category success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CAT_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found category", ErrorCode.CATEGORY_ERROR_NOT_FOUND));
        if (StringUtils.isNotBlank(category.getAvatar())) {
            fileService.deleteFile(category.getAvatar());
        }
        categoryRepository.delete(category);
        return makeSuccessResponse("Delete category success");
    }
}
