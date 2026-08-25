package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.file.UploadFileDto;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.file.UploadFileForm;
import auto.ui.api.form.file.UploadPageFileForm;
import auto.ui.api.repository.PageRepository;
import auto.ui.api.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/file")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class FileController {
    @Autowired
    private FileService fileService;

    @Autowired
    private PageRepository pageRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('FILE_U')")
    public ApiMessageDto<UploadFileDto> upload(@Valid UploadFileForm uploadFileForm, BindingResult bindingResult) {
        ApiMessageDto<UploadFileDto> apiMessageDto = fileService.storeFile(uploadFileForm);
        apiMessageDto.setResult(true);
        return apiMessageDto;
    }

    @PostMapping(value = "/upload-page", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('FILE_U')")
    public ApiMessageDto<UploadFileDto> uploadPage(@Valid UploadPageFileForm uploadPageFileForm, BindingResult bindingResult) {
        if (!pageRepository.existsById(uploadPageFileForm.getPageId())) {
            throw new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND);
        }
        String typeFolder = File.separator + FileService.PAGE_TYPE + File.separator + uploadPageFileForm.getPageId();
        ApiMessageDto<UploadFileDto> apiMessageDto = fileService.store(uploadPageFileForm.getFile(), FileService.PAGE_TYPE, true, typeFolder);
        apiMessageDto.setResult(true);
        return apiMessageDto;
    }

    @GetMapping("/download/{folder}/{fileName:.+}")
    @Cacheable("images")
    public ResponseEntity<Resource> downloadFile(@PathVariable String folder, @PathVariable String fileName, HttpServletRequest request) throws FileNotFoundException {
        Resource resource = fileService.loadFileAsResource(folder, fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7776000, TimeUnit.SECONDS))
                .contentType(MediaType.parseMediaType(contentType))
                //.header(HttpHeaders.EXPIRES, expires)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/download/{folder}/{pageId}/{fileName:.+}")
    @Cacheable("images")
    public ResponseEntity<Resource> downloadPageFile(@PathVariable String folder, @PathVariable String pageId, @PathVariable String fileName, HttpServletRequest request) throws FileNotFoundException {
        Resource resource = fileService.loadFileAsResource(folder, pageId, fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7776000, TimeUnit.SECONDS))
                .contentType(MediaType.parseMediaType(contentType))
                //.header(HttpHeaders.EXPIRES, expires)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
