package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.page.PageDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.page.PublishPageForm;
import auto.ui.api.form.page.AutoSavePageForm;
import auto.ui.api.mapper.PageMapper;
import auto.ui.api.model.Page;
import auto.ui.api.repository.PageRepository;
import auto.ui.api.service.impl.UserServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    private static final Long PAGE_ID = 1L;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageMapper pageMapper;

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private PageController pageController;

    private Page page(Long version) {
        Page page = new Page();
        page.setId(PAGE_ID);
        page.setName("Trang demo");
        page.setSlug("demo");
        page.setVersion(version);
        return page;
    }

    private JsonNode json(String raw) throws JsonProcessingException {
        return new ObjectMapper().readTree(raw);
    }

    @Test
    void shouldReturnPageDtoWhenIdExists() {
        Page page = page(0L);
        PageDto dto = new PageDto();
        dto.setId(PAGE_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPageDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = pageController.get(PAGE_ID);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get page success");
    }

    @Test
    void shouldThrowNotFoundWhenPageIdDoesNotExist() {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pageController.get(PAGE_ID))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNewVersionWhenAutosaveVersionMatches() throws JsonProcessingException {
        Page page = page(3L);
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(PAGE_ID);
        form.setVersion(3L);
        form.setProjectData(json("{}"));

        PageDto dto = new PageDto();
        dto.setId(PAGE_ID);
        dto.setVersion(4L);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPageIdDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = pageController.autosave(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getVersion()).isEqualTo(4L);
        assertThat(result.getMessage()).isEqualTo("Autosave page success");
        verify(pageMapper).autoSaveEntityFromForm(form, page);
        verify(pageRepository).saveAndFlush(page);
    }

    @Test
    void shouldThrowBadRequestWhenAutosaveVersionIsStale() throws JsonProcessingException {
        Page page = page(5L);
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(PAGE_ID);
        form.setVersion(3L);
        form.setProjectData(json("{}"));
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> pageController.autosave(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_VERSION_CONFLICT);
        verify(pageRepository, never()).saveAndFlush(any(Page.class));
    }

    @Test
    void shouldThrowNotFoundWhenAutosaveTargetDoesNotExist() {
        AutoSavePageForm form = new AutoSavePageForm();
        form.setId(PAGE_ID);
        form.setVersion(0L);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pageController.autosave(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldSavePageConfigWhenEveryBlockTypeIsAllowed() throws JsonProcessingException {
        Page page = page(1L);
        PublishPageForm form = new PublishPageForm();
        form.setId(PAGE_ID);
        form.setConfig(json("{\"blocks\":[{\"type\":\"hero\"},{\"type\":\"cta\"}]}"));

        PageDto dto = new PageDto();
        dto.setId(PAGE_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPageIdDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = pageController.publish(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Publish page success");
        assertThat(page.getPublishedAt()).isNotNull();
        verify(pageMapper).publishEntityFromForm(form, page);
        verify(pageRepository).saveAndFlush(page);
    }

    @Test
    void shouldThrowBadRequestWhenBlockTypeIsNotWhitelisted() throws JsonProcessingException {
        Page page = page(1L);
        PublishPageForm form = new PublishPageForm();
        form.setId(PAGE_ID);
        form.setConfig(json("{\"blocks\":[{\"type\":\"hero\"},{\"type\":\"evil\"}]}"));
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> pageController.publish(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_INVALID_BLOCK);
        verify(pageRepository, never()).saveAndFlush(any(Page.class));
    }

    @Test
    void shouldThrowBadRequestWhenConfigBlocksIsNotAnArray() throws JsonProcessingException {
        Page page = page(1L);
        PublishPageForm form = new PublishPageForm();
        form.setId(PAGE_ID);
        form.setConfig(json("{\"blocks\":\"hero\"}"));
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> pageController.publish(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_INVALID_BLOCK);
    }

    @Test
    void shouldThrowNotFoundWhenPublishTargetDoesNotExist() {
        PublishPageForm form = new PublishPageForm();
        form.setId(PAGE_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pageController.publish(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnPublicPageDtoWhenSlugIsPublished() {
        Page page = page(1L);
        page.setPageConfig("{\"blocks\":[]}");
        PageDto dto = new PageDto();
        dto.setSlug("demo");
        when(pageRepository.findFirstBySlug("demo")).thenReturn(Optional.of(page));
        when(pageMapper.fromEntityToPublicPageDto(page)).thenReturn(dto);

        ApiMessageDto<PageDto> result = pageController.publicGet("demo");

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get public page success");
    }

    @Test
    void shouldThrowNotFoundWhenSlugHasNeverBeenPublished() {
        Page page = page(1L);
        page.setPageConfig(null);
        when(pageRepository.findFirstBySlug("demo")).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> pageController.publicGet("demo"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_PUBLISHED);
    }

    @Test
    void shouldThrowNotFoundWhenSlugDoesNotExist() {
        when(pageRepository.findFirstBySlug("khong-co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pageController.publicGet("khong-co"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.PAGE_ERROR_NOT_FOUND);
    }
}
