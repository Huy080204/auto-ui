package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.page.PageDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.page.AutoSavePageForm;
import auto.ui.api.form.page.CreateDraftPageForm;
import auto.ui.api.form.page.CreatePageForm;
import auto.ui.api.form.page.PublicVersionPageForm;
import auto.ui.api.form.page.UpdatePageForm;
import auto.ui.api.mapper.PageMapper;
import auto.ui.api.model.Pages;
import auto.ui.api.model.criteria.PageCriteria;
import auto.ui.api.repository.PageRepository;
import auto.ui.api.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/page")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class PageController extends ABasicController {
    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private FileService fileService;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<PageDto> get(@PathVariable Long id) {
        Pages pages = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        PageDto pageDto = pageMapper.fromEntityToPageDto(pages);
        if (Boolean.TRUE.equals(pages.getIsHasDraft())) {
            pageRepository.findFirstByActiveVersionId(pages.getId())
                    .ifPresent(draft -> pageDto.setDraftPage(pageMapper.fromEntityToPageDto(draft)));
        }
        return makeSuccessResponse(pageDto, "Get page success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<PageDto>>> list(PageCriteria pageCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageCriteria.getIsDraft() == null) {
            pageCriteria.setIsDraft(false);
        }
        Page<Pages> pages = pageRepository.findAll(pageCriteria.getCriteria(), pageable);
        ResponseListDto<List<PageDto>> responseListDto =
                makeResponseListDto(pages, pageMapper::fromEntityListToPageDtoList);
        return makeSuccessResponse(responseListDto, "Get list page success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<PageDto>>> autoComplete(PageCriteria pageCriteria, Pageable pageable) {
        pageCriteria.setIsDraft(false);
        Page<Pages> pages = pageRepository.findAll(pageCriteria.getCriteria(), pageable);
        return makeSuccessResponse(makeResponseListDto(pages, pageMapper::fromEntityListToPageDtoAutoComplete),
                "Get auto complete page success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> create(@Valid @RequestBody CreatePageForm createPageForm, BindingResult bindingResult) {
        if (pageRepository.existsBySlug(createPageForm.getSlug())) {
            throw new BadRequestException("Page slug already exist", ErrorCode.PAGE_ERROR_SLUG_EXIST);
        }
        Pages pages = pageMapper.fromFormToEntity(createPageForm);
        pages.setIsDefault(false);
        pageRepository.save(pages);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(pages), "Create page success");
    }

    /** Chỉ đổi được name — slug bất biến, xem javadoc UpdatePageForm. Chỉ cho phép trên bản draft. */
    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdatePageForm updatePageForm, BindingResult bindingResult) {
        Pages pages = pageRepository.findById(updatePageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (!Boolean.TRUE.equals(pages.getIsDraft())) {
            throw new BadRequestException("Cannot update a page that is not a draft", ErrorCode.PAGE_ERROR_UPDATE_NOT_ALLOWED);
        }
        pageMapper.updateEntityFromForm(updatePageForm, pages);
        pageRepository.save(pages);
        return makeSuccessResponse("Update page success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable("id") Long id) {
        Pages pages = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (Boolean.TRUE.equals(pages.getIsDraft()) && pages.getActiveVersion() != null) {
            Pages activeVersion = pages.getActiveVersion();
            activeVersion.setIsHasDraft(false);
            pageRepository.save(activeVersion);
        }
        if (Boolean.TRUE.equals(pages.getIsHasDraft())) {
            pageRepository.findFirstByActiveVersionId(pages.getId())
                    .ifPresent(draft -> fileService.deletePageFolder(draft.getId()));
            pageRepository.deleteAllByActiveVersionId(pages.getId());
        }
        pageRepository.delete(pages);
        fileService.deletePageFolder(id);
        return makeSuccessResponse("Delete page success");
    }

    /** Autosave từ editor. Chỉ cho phép trên bản draft. */
    @PutMapping(value = "/autosave", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> autosave(@Valid @RequestBody AutoSavePageForm autoSavePageForm, BindingResult bindingResult) {
        Pages pages = pageRepository.findById(autoSavePageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (!Boolean.TRUE.equals(pages.getIsDraft())) {
            throw new BadRequestException("Cannot autosave a page that is not a draft", ErrorCode.PAGE_ERROR_UPDATE_NOT_ALLOWED);
        }
        pageMapper.autoSaveEntityFromForm(autoSavePageForm, pages);
        pageRepository.save(pages);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(pages), "Autosave page success");
    }

    /** Clone 1 page active thành bản nháp mới — chặn nếu đã có draft trỏ tới nó. */
    @PostMapping(value = "/create-draft", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> createDraft(@Valid @RequestBody CreateDraftPageForm createDraftPageForm, BindingResult bindingResult) {
        Pages pages = pageRepository.findById(createDraftPageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (Boolean.TRUE.equals(pages.getIsDraft())) {
            throw new BadRequestException("Page is already a draft", ErrorCode.PAGE_ERROR_ALREADY_DRAFT);
        }
        if (pageRepository.existsByActiveVersionId(pages.getId())) {
            throw new BadRequestException("Draft already exists for this page", ErrorCode.PAGE_ERROR_DRAFT_EXISTS);
        }
        Pages draft = pageMapper.fromEntityToDraft(pages);
        draft.setIsDraft(true);
        draft.setActiveVersion(pages);
        draft.setIsDefault(false);
        pageRepository.save(draft);
        pages.setIsHasDraft(true);
        pageRepository.save(pages);
        return makeSuccessResponse(pageMapper.fromEntityToPageIdDto(draft), "Create draft page success");
    }

    /** Promote 1 bản draft thành active version — merge nội dung lên row active rồi xoá draft. */
    @PostMapping(value = "/public-version", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> publicVersion(@Valid @RequestBody PublicVersionPageForm publicVersionPageForm, BindingResult bindingResult) {
        Pages draft = pageRepository.findById(publicVersionPageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new BadRequestException("Page is not a draft", ErrorCode.PAGE_ERROR_NOT_DRAFT);
        }
        Pages activeVersion = draft.getActiveVersion();
        if (activeVersion == null) {
            throw new NotFoundException("Not found active version of this page", ErrorCode.PAGE_ERROR_NOT_FOUND);
        }
        pageMapper.updateEntityFromDraft(draft, activeVersion);
        activeVersion.setIsHasDraft(false);
        pageRepository.save(activeVersion);
        pageRepository.delete(draft);
        return makeSuccessResponse("Promote draft to active version success");
    }

    /** Đặt 1 page làm default — tự động unset page default trước đó, đảm bảo chỉ 1 page default. */
    @PutMapping(value = "/set-default/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> setDefault(@PathVariable("id") Long id) {
        Pages pages = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        pageRepository.unsetDefaultExcept(pages.getId());
        pages.setIsDefault(true);
        pageRepository.save(pages);
        return makeSuccessResponse("Set default page success");
    }

    @GetMapping(value = "/public/get/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<PageDto> publicGet(@PathVariable String slug) {
        Pages pages = pageRepository.findFirstBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (Boolean.TRUE.equals(pages.getIsDraft())) {
            throw new NotFoundException("Page is not published yet", ErrorCode.PAGE_ERROR_NOT_PUBLISHED);
        }
        return makeSuccessResponse(pageMapper.fromEntityToPublicPageDto(pages), "Get public page success");
    }
}
