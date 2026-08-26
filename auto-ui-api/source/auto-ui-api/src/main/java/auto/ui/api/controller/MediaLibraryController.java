package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.file.UploadFileDto;
import auto.ui.api.dto.medialibrary.MediaLibraryDto;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.medialibrary.CreateMediaLibraryForm;
import auto.ui.api.form.medialibrary.UpdateMediaLibraryForm;
import auto.ui.api.mapper.MediaLibraryMapper;
import auto.ui.api.model.MediaLibrary;
import auto.ui.api.model.criteria.MediaLibraryCriteria;
import auto.ui.api.repository.MediaLibraryRepository;
import auto.ui.api.service.FileService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/media-library")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class MediaLibraryController extends ABasicController {
    @Autowired
    private MediaLibraryRepository mediaLibraryRepository;

    @Autowired
    private MediaLibraryMapper mediaLibraryMapper;

    @Autowired
    private FileService fileService;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<MediaLibraryDto>>> list(MediaLibraryCriteria mediaLibraryCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MediaLibrary> page = mediaLibraryRepository.findAll(mediaLibraryCriteria.getCriteria(), pageable);
        ResponseListDto<List<MediaLibraryDto>> responseListDto =
                makeResponseListDto(page, mediaLibraryMapper::fromEntityToMediaLibraryDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<MediaLibraryDto> create(@Valid CreateMediaLibraryForm createMediaLibraryForm, BindingResult bindingResult) {
        ApiMessageDto<UploadFileDto> uploadResult = fileService.storeMediaLibraryFile(createMediaLibraryForm.getFile());

        MediaLibrary mediaLibrary = new MediaLibrary();
        mediaLibrary.setUrl(uploadResult.getData().getFilePath());
        mediaLibraryRepository.save(mediaLibrary);
        return makeSuccessResponse(mediaLibraryMapper.fromEntityToMediaLibraryIdDto(mediaLibrary), "Create media library success");
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> update(@Valid UpdateMediaLibraryForm updateMediaLibraryForm, BindingResult bindingResult) {
        MediaLibrary mediaLibrary = mediaLibraryRepository.findById(updateMediaLibraryForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found media library!", ErrorCode.MEDIA_LIBRARY_ERROR_NOT_FOUND));

        ApiMessageDto<UploadFileDto> uploadResult = fileService.updateMediaLibraryFile(updateMediaLibraryForm.getFile(), mediaLibrary.getUrl());
        mediaLibrary.setUrl(uploadResult.getData().getFilePath());
        mediaLibraryRepository.save(mediaLibrary);
        return makeSuccessResponse("Update media library success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        MediaLibrary mediaLibrary = mediaLibraryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found media library!", ErrorCode.MEDIA_LIBRARY_ERROR_NOT_FOUND));
        mediaLibraryRepository.delete(mediaLibrary);
        if (mediaLibrary.getUrl() != null && !mediaLibrary.getUrl().trim().isEmpty()) {
            fileService.deleteFile(mediaLibrary.getUrl());
        }
        return makeSuccessResponse("Delete media library success");
    }
}
