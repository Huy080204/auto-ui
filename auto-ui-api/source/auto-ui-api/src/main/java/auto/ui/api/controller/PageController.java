package auto.ui.api.controller;

import auto.ui.api.constant.PageConstant;
import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.page.PageDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.page.AutoSavePageForm;
import auto.ui.api.form.page.CreatePageForm;
import auto.ui.api.form.page.PublishPageForm;
import auto.ui.api.form.page.UpdatePageForm;
import auto.ui.api.mapper.PageMapper;
import auto.ui.api.model.Page;
import auto.ui.api.model.criteria.PageCriteria;
import auto.ui.api.repository.PageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/v1/page")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class PageController extends ABasicController {
    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageMapper pageMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<PageDto> get(@PathVariable Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        return makeSuccessResponse(pageMapper.fromEntityToPageDto(page), "Get page success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PAG_L')")
    public ApiMessageDto<ResponseListDto<List<PageDto>>> list(PageCriteria pageCriteria,
                                                              @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        org.springframework.data.domain.Page<Page> pages =
                pageRepository.findAll(pageCriteria.getCriteria(), pageable);
        ResponseListDto<List<PageDto>> responseListDto =
                makeResponseListDto(pages, pageMapper::fromEntityListToPageDtoList);
        return makeSuccessResponse(responseListDto, "Get list page success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<PageDto>>> autoComplete(PageCriteria pageCriteria, Pageable pageable) {
        org.springframework.data.domain.Page<Page> pages =
                pageRepository.findAll(pageCriteria.getCriteria(), pageable);
        return makeSuccessResponse(makeResponseListDto(pages, pageMapper::fromEntityListToPageDtoAutoComplete),
                "Get auto complete page success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PAG_C')")
    @Transactional
    public ApiMessageDto<PageDto> create(@Valid @RequestBody CreatePageForm createPageForm, BindingResult bindingResult) {
        if (pageRepository.existsBySlug(createPageForm.getSlug())) {
            throw new BadRequestException("Page slug already exist", ErrorCode.PAGE_ERROR_SLUG_EXIST);
        }
        Page page = pageMapper.fromFormToEntity(createPageForm);
        pageRepository.save(page);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(page), "Create page success");
    }

    /** Chỉ đổi được name — slug bất biến, xem javadoc UpdatePageForm. */
    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PAG_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdatePageForm updatePageForm, BindingResult bindingResult) {
        Page page = pageRepository.findById(updatePageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        pageMapper.updateEntityFromForm(updatePageForm, page);
        pageRepository.save(page);
        return makeSuccessResponse("Update page success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PAG_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable("id") Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        pageRepository.delete(page);
        return makeSuccessResponse("Delete page success");
    }

    /**
     * Autosave từ editor. Chỉ ghi project_data, không đụng page_config —
     * trang công khai chỉ đổi khi publish.
     */
    @PutMapping(value = "/autosave", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> autosave(@Valid @RequestBody AutoSavePageForm autoSavePageForm, BindingResult bindingResult) {
        Page page = pageRepository.findById(autoSavePageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (!Objects.equals(page.getVersion(), autoSavePageForm.getVersion())) {
            throw new BadRequestException("Page was modified elsewhere, current version is "
                    + page.getVersion(), ErrorCode.PAGE_ERROR_VERSION_CONFLICT);
        }
        pageMapper.autoSaveEntityFromForm(autoSavePageForm, page);
        pageRepository.saveAndFlush(page);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(page), "Autosave page success");
    }

    /**
     * Publish: ghi page_config + published_at. Đừng tin client — mọi block type
     * không nằm trong whitelist đều bị chặn ở đây (PLAN.md mục 8).
     */
    @PostMapping(value = "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> publish(@Valid @RequestBody PublishPageForm publishPageForm, BindingResult bindingResult) {
        Page page = pageRepository.findById(publishPageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        validateBlockTypes(publishPageForm.getConfig());
        pageMapper.publishEntityFromForm(publishPageForm, page);
        page.setPublishedAt(new Date());
        pageRepository.saveAndFlush(page);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(page), "Publish page success");
    }

    @GetMapping(value = "/public/get/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<PageDto> publicGet(@PathVariable String slug) {
        Page page = pageRepository.findFirstBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (page.getPageConfig() == null) {
            throw new NotFoundException("Page is not published yet", ErrorCode.PAGE_ERROR_NOT_PUBLISHED);
        }
        return makeSuccessResponse(pageMapper.fromEntityToPublicPageDto(page), "Get public page success");
    }

    private void validateBlockTypes(JsonNode config) {
        JsonNode blocks = config.path("blocks");
        if (!blocks.isArray()) {
            throw new BadRequestException("config.blocks must be an array", ErrorCode.PAGE_ERROR_INVALID_BLOCK);
        }
        for (JsonNode block : blocks) {
            String type = block.path("type").asText();
            if (!PageConstant.ALLOWED_BLOCK_TYPES.contains(type)) {
                throw new BadRequestException("Block type is not allowed: " + type, ErrorCode.PAGE_ERROR_INVALID_BLOCK);
            }
        }
    }
}
