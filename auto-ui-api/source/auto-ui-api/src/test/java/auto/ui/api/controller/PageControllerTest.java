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
import auto.ui.api.model.Page;
import auto.ui.api.model.criteria.PageCriteria;
import auto.ui.api.repository.PageRepository;
import auto.ui.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock
    private PageRepository pageRepository;
    @Mock
    private PageMapper pageMapper;
    @Mock
    private UserServiceImpl userService;
    @InjectMocks
    private PageController controller;

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnPageDtoWhenGetIdExists() {
        Page page = new Page();
        PageDto dto = new PageDto();
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPageDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get page success");
    }

    @Test
    void shouldReturnListWhenListCalled() {
        PageCriteria criteria = new PageCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Page> springPage =
                new PageImpl<>(Collections.singletonList(new Page()));
        when(pageRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(springPage);
        when(pageMapper.fromEntityListToPageDtoList(springPage.getContent()))
                .thenReturn(Collections.singletonList(new PageDto()));

        ApiMessageDto<ResponseListDto<java.util.List<PageDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getData().getTotalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnListWhenAutoCompleteCalled() {
        PageCriteria criteria = new PageCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Page> springPage =
                new PageImpl<>(Collections.singletonList(new Page()));
        when(pageRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(springPage);
        when(pageMapper.fromEntityListToPageDtoAutoComplete(springPage.getContent()))
                .thenReturn(Collections.singletonList(new PageDto()));

        ApiMessageDto<ResponseListDto<java.util.List<PageDto>>> result = controller.autoComplete(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).hasSize(1);
    }

    @Test
    void shouldThrowBadRequestWhenCreateSlugAlreadyExists() {
        CreatePageForm form = new CreatePageForm();
        form.setName("Home");
        form.setSlug("home");
        when(pageRepository.existsBySlug("home")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_SLUG_EXIST);
    }

    @Test
    void shouldCreatePageWhenSlugIsUnique() {
        CreatePageForm form = new CreatePageForm();
        form.setName("Home");
        form.setSlug("home");
        Page entity = new Page();
        PageDto dto = new PageDto();
        when(pageRepository.existsBySlug("home")).thenReturn(false);
        when(pageMapper.fromFormToEntity(form)).thenReturn(entity);
        when(pageMapper.fromEntityToPageIdDto(entity)).thenReturn(dto);

        ApiMessageDto<PageDto> result = controller.create(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(entity.getIsDraft()).isFalse();
        assertThat(entity.getIsDefault()).isFalse();
        verify(pageRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateIdDoesNotExist() {
        UpdatePageForm form = new UpdatePageForm();
        form.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdatingPageThatIsNotDraft() {
        UpdatePageForm form = new UpdatePageForm();
        form.setId(1L);
        Page page = new Page();
        page.setIsDraft(false);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_UPDATE_NOT_ALLOWED);
    }

    @Test
    void shouldUpdatePageWhenPageIsDraft() {
        UpdatePageForm form = new UpdatePageForm();
        form.setId(1L);
        form.setName("New name");
        Page page = new Page();
        page.setIsDraft(true);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        ApiMessageDto<Void> result = controller.update(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update page success");
        verify(pageMapper).updateEntityFromForm(form, page);
        verify(pageRepository).save(page);
    }

    @Test
    void shouldThrowNotFoundWhenDeleteIdDoesNotExist() {
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeletePageWhenIdExists() {
        Page page = new Page();
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        verify(pageRepository).delete(page);
    }

    @Test
    void shouldThrowNotFoundWhenAutosaveIdDoesNotExist() {
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.autosave(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenAutosavingPageThatIsNotDraft() {
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(1L);
        Page page = new Page();
        page.setIsDraft(false);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> controller.autosave(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_UPDATE_NOT_ALLOWED);
    }

    @Test
    void shouldAutosavePageWhenPageIsDraft() {
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(1L);
        Page page = new Page();
        page.setIsDraft(true);
        PageDto dto = new PageDto();
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPageIdDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = controller.autosave(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        verify(pageMapper).autoSaveEntityFromForm(form, page);
        verify(pageRepository).saveAndFlush(page);
    }

    @Test
    void shouldThrowNotFoundWhenCreateDraftIdDoesNotExist() {
        CreateDraftPageForm form = new CreateDraftPageForm();
        form.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.createDraft(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenCreateDraftFromPageThatIsAlreadyDraft() {
        CreateDraftPageForm form = new CreateDraftPageForm();
        form.setId(1L);
        Page page = new Page();
        page.setIsDraft(true);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> controller.createDraft(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_ALREADY_DRAFT);
    }

    @Test
    void shouldThrowBadRequestWhenCreateDraftAndDraftAlreadyExists() {
        CreateDraftPageForm form = new CreateDraftPageForm();
        form.setId(1L);
        Page page = new Page();
        page.setId(1L);
        page.setIsDraft(false);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageRepository.existsByActiveVersionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.createDraft(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_DRAFT_EXISTS);
    }

    @Test
    void shouldCreateDraftWhenActivePageHasNoExistingDraft() {
        CreateDraftPageForm form = new CreateDraftPageForm();
        form.setId(1L);
        Page page = new Page();
        page.setId(1L);
        page.setIsDraft(false);
        Page draft = new Page();
        PageDto dto = new PageDto();
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageRepository.existsByActiveVersionId(1L)).thenReturn(false);
        when(pageMapper.fromEntityToDraft(page)).thenReturn(draft);
        when(pageMapper.fromEntityToPageIdDto(draft)).thenReturn(dto);

        ApiMessageDto<PageDto> result = controller.createDraft(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(draft.getIsDraft()).isTrue();
        assertThat(draft.getActiveVersion()).isSameAs(page);
        assertThat(draft.getIsDefault()).isFalse();
        verify(pageRepository).save(draft);
    }

    @Test
    void shouldThrowNotFoundWhenPublicVersionIdDoesNotExist() {
        PublicVersionPageForm form = new PublicVersionPageForm();
        form.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.publicVersion(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenPublicVersionPageIsNotDraft() {
        PublicVersionPageForm form = new PublicVersionPageForm();
        form.setId(1L);
        Page page = new Page();
        page.setIsDraft(false);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> controller.publicVersion(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_DRAFT);
    }

    @Test
    void shouldThrowNotFoundWhenPublicVersionDraftHasNoActiveVersion() {
        PublicVersionPageForm form = new PublicVersionPageForm();
        form.setId(1L);
        Page draft = new Page();
        draft.setIsDraft(true);
        draft.setActiveVersion(null);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> controller.publicVersion(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldPromoteDraftToActiveVersionWhenDraftIsValid() {
        PublicVersionPageForm form = new PublicVersionPageForm();
        form.setId(2L);
        Page activeVersion = new Page();
        Page draft = new Page();
        draft.setIsDraft(true);
        draft.setActiveVersion(activeVersion);
        when(pageRepository.findById(2L)).thenReturn(Optional.of(draft));

        ApiMessageDto<Void> result = controller.publicVersion(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Promote draft to active version success");
        verify(pageMapper).updateEntityFromDraft(draft, activeVersion);
        verify(pageRepository).save(activeVersion);
        verify(pageRepository).delete(draft);
    }

    @Test
    void shouldThrowNotFoundWhenSetDefaultIdDoesNotExist() {
        when(pageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.setDefault(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldSetDefaultAndUnsetPreviousDefaultWhenIdExists() {
        Page page = new Page();
        page.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        ApiMessageDto<Void> result = controller.setDefault(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(page.getIsDefault()).isTrue();
        verify(pageRepository).unsetDefaultExcept(1L);
        verify(pageRepository).save(page);
    }

    @Test
    void shouldThrowNotFoundWhenPublicGetSlugDoesNotExist() {
        when(pageRepository.findFirstBySlug("home")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.publicGet("home"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowNotFoundWhenPublicGetPageIsStillDraft() {
        Page page = new Page();
        page.setIsDraft(true);
        when(pageRepository.findFirstBySlug("home")).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> controller.publicGet("home"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_PUBLISHED);
    }

    @Test
    void shouldReturnPublicPageDtoWhenPageIsActive() {
        Page page = new Page();
        page.setIsDraft(false);
        PageDto dto = new PageDto();
        when(pageRepository.findFirstBySlug("home")).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPublicPageDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = controller.publicGet("home");

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
    }
}
