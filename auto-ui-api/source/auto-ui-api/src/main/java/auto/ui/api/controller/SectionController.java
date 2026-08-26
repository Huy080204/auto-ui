package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.section.SectionDto;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.section.CreateSectionForm;
import auto.ui.api.form.section.UpdateSectionForm;
import auto.ui.api.mapper.SectionMapper;
import auto.ui.api.model.Section;
import auto.ui.api.model.criteria.SectionCriteria;
import auto.ui.api.repository.SectionRepository;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/v1/section")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class SectionController extends ABasicController {
    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SectionMapper sectionMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<SectionDto> get(@PathVariable Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found section!", ErrorCode.SECTION_ERROR_NOT_FOUND));
        return makeSuccessResponse(sectionMapper.fromEntityToSectionDto(section), "Get section success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<SectionDto>>> list(SectionCriteria sectionCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Section> page = sectionRepository.findAll(sectionCriteria.getCriteria(), pageable);
        ResponseListDto<List<SectionDto>> responseListDto =
                makeResponseListDto(page, sectionMapper::fromEntityToSectionDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<SectionDto> create(@Valid @RequestBody CreateSectionForm createSectionForm, BindingResult bindingResult) {
        Section section = sectionMapper.fromFormToEntity(createSectionForm);
        sectionRepository.save(section);
        return makeSuccessResponse(sectionMapper.fromEntityToSectionIdDto(section), "Create section success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateSectionForm updateSectionForm, BindingResult bindingResult) {
        Section section = sectionRepository.findById(updateSectionForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found section!", ErrorCode.SECTION_ERROR_NOT_FOUND));
        sectionMapper.updateEntityFromForm(updateSectionForm, section);
        sectionRepository.save(section);
        return makeSuccessResponse("Update section success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found section!", ErrorCode.SECTION_ERROR_NOT_FOUND));
        sectionRepository.delete(section);
        return makeSuccessResponse("Delete section success");
    }
}
