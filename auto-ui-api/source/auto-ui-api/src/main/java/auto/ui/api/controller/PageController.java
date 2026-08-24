package auto.ui.api.controller;

import auto.ui.api.constant.PageConstant;
import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.page.PageDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.page.PublishPageForm;
import auto.ui.api.form.page.UpdatePageForm;
import auto.ui.api.mapper.PageMapper;
import auto.ui.api.model.Page;
import auto.ui.api.repository.PageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;
import java.util.Objects;

/**
 * Trang GrapesJS: editor load/autosave/publish, Next.js đọc bản đã publish.
 *
 * Lệch có chủ đích so với itz-controller-conventions.md, đã thống nhất với chủ dự án:
 * - Không @PreAuthorize: bản demo bỏ auth (PLAN.md mục 4), editor chưa có màn login.
 *   Thay vào đó cả nhánh /v1/page/** được mở trong ResourceServerConfig.
 * - Không có create/delete/list: phạm vi demo chỉ 4 endpoint trong PLAN.md mục 4,
 *   nên cũng không có Criteria lẫn JpaSpecificationExecutor.
 * - /autosave và /publish là action riêng, không phải /update của bộ CRUD chuẩn:
 *   autosave chỉ ghi một cột opaque và phải trả version mới về cho optimistic lock.
 */
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
    public ApiMessageDto<PageDto> get(@PathVariable("id") Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        return makeSuccessResponse(pageMapper.fromEntityToPageDto(page), "Get page success");
    }

    /**
     * Autosave từ editor. Chỉ ghi project_data, không đụng page_config —
     * trang công khai chỉ đổi khi publish.
     */
    @PutMapping(value = "/autosave", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<PageDto> autosave(@Valid @RequestBody UpdatePageForm updatePageForm, BindingResult bindingResult) {
        Page page = pageRepository.findById(updatePageForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND));
        if (!Objects.equals(page.getVersion(), updatePageForm.getVersion())) {
            throw new BadRequestException("Page was modified elsewhere, current version is "
                    + page.getVersion(), ErrorCode.PAGE_ERROR_VERSION_CONFLICT);
        }
        pageMapper.updateEntityFromForm(updatePageForm, page);
        // saveAndFlush: @Version chỉ tăng khi flush, mà editor cần version mới ngay trong response
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

    /** Next.js đọc trang công khai theo slug — chỉ trả bản đã publish. */
    @GetMapping(value = "/public/get/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<PageDto> publicGet(@PathVariable("slug") String slug) {
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
