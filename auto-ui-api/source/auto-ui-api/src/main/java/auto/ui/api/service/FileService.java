package auto.ui.api.service;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.file.UploadFileDto;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.form.file.UploadFileForm;
import auto.ui.api.repository.PageRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FileService {
    static final String[] UPLOAD_TYPES = new String[]{"AVATAR", "LOGO", "SETTING"};
    static final String[] AVATAR_EXTENSION = new String[]{"jpeg", "jpg", "gif", "bmp", "png"};

    @Value("${file.upload-dir}")
    private String ROOT_DIRECTORY;

    @Autowired
    private PageRepository pageRepository;

    public ApiMessageDto<UploadFileDto> storeFile(UploadFileForm uploadFileForm) {
        boolean contains = Arrays.stream(UPLOAD_TYPES).anyMatch(uploadFileForm.getType()::equalsIgnoreCase);
        if (!contains) {
            throw new BadRequestException("ERROR-UPLOAD-TYPE-INVALID", "Type is required in AVATAR, LOGO, SETTING");
        }
        boolean checkExtension = uploadFileForm.getType().equals("AVATAR") || uploadFileForm.getType().equals("LOGO");
        String typeFolder = File.separator + uploadFileForm.getType();
        return store(uploadFileForm.getFile(), uploadFileForm.getType(), checkExtension, typeFolder);
    }

    public ApiMessageDto<UploadFileDto> storePageFile(MultipartFile file, Long pageId) {
        if (!pageRepository.existsById(pageId)) {
            throw new NotFoundException("Not found Page", ErrorCode.PAGE_ERROR_NOT_FOUND);
        }
        String typeFolder = File.separator + AIConstant.FILE_UPLOAD_TYPE_PAGE + File.separator + pageId;
        return store(file, AIConstant.FILE_UPLOAD_TYPE_PAGE, true, typeFolder);
    }

    public ApiMessageDto<UploadFileDto> storeMediaLibraryFile(MultipartFile file) {
        String typeFolder = File.separator + AIConstant.FILE_UPLOAD_TYPE_MEDIA_LIBRARY;
        ApiMessageDto<UploadFileDto> uploadResult = store(file, AIConstant.FILE_UPLOAD_TYPE_MEDIA_LIBRARY, true, typeFolder);
        if (Boolean.FALSE.equals(uploadResult.getResult())) {
            throw new BadRequestException(uploadResult.getMessage(), ErrorCode.MEDIA_LIBRARY_ERROR_UPLOAD_FAILED);
        }
        return uploadResult;
    }

    public ApiMessageDto<UploadFileDto> updateMediaLibraryFile(MultipartFile file, String existingUrl) {
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if (!Arrays.stream(AVATAR_EXTENSION).anyMatch(extension::equalsIgnoreCase)) {
                throw new BadRequestException("ERROR-FILE-FORMAT-INVALID", "File format is invalid");
            }

            Path targetLocation = Paths.get(ROOT_DIRECTORY + existingUrl).toAbsolutePath().normalize();
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(existingUrl);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("" + e.getMessage());
        }

        if (Boolean.FALSE.equals(apiMessageDto.getResult())) {
            throw new BadRequestException(apiMessageDto.getMessage(), ErrorCode.MEDIA_LIBRARY_ERROR_UPLOAD_FAILED);
        }
        return apiMessageDto;
    }

    public ApiMessageDto<UploadFileDto> store(MultipartFile file, String type, boolean checkExtension, String typeFolder) {
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = FilenameUtils.getExtension(fileName);
            if (checkExtension && !Arrays.stream(AVATAR_EXTENSION).anyMatch(extension::equalsIgnoreCase)) {
                throw new BadRequestException("ERROR-FILE-FORMAT-INVALID", "File format is invalid");
            }
            String finalFile = type + "_" + RandomStringUtils.randomAlphanumeric(10) + "." + extension;

            Path fileStorageLocation = Paths.get(ROOT_DIRECTORY + typeFolder).toAbsolutePath().normalize();
            Files.createDirectories(fileStorageLocation);
            Path targetLocation = fileStorageLocation.resolve(finalFile);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(typeFolder + File.separator + finalFile);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("" + e.getMessage());
        }
        return apiMessageDto;
    }

    public Resource loadFileAsResource(String folder, String fileName) {

        try {
            Path fileStorageLocation = Paths.get(ROOT_DIRECTORY + File.separator + folder).toAbsolutePath().normalize();
            Path fP = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(fP.toUri());
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            log.error(ex.getMessage(), ex);

        }
        return null;
    }

    public Resource loadFileAsResource(String folder, String pageId, String fileName) {
        try {
            Path fileStorageLocation = Paths.get(ROOT_DIRECTORY + File.separator + folder + File.separator + pageId).toAbsolutePath().normalize();
            Path fP = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(fP.toUri());
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            log.error(ex.getMessage(), ex);

        }
        return null;
    }

    public void deleteFile(String filePath) {
        if (!isDeletable(filePath)) {
            return;
        }
        File file = new File(ROOT_DIRECTORY + filePath);
        file.delete();
    }

    public void deleteFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            if (!isDeletable(filePath)) {
                continue;
            }
            File file = new File(ROOT_DIRECTORY + filePath);
            file.delete();
        }
    }

    public void deletePageFolder(Long pageId) {
        if (pageId == null) {
            return;
        }
        File folder = new File(ROOT_DIRECTORY + File.separator + AIConstant.FILE_UPLOAD_TYPE_PAGE + File.separator + pageId);
        if (folder.isDirectory()) {
            FileSystemUtils.deleteRecursively(folder);
        }
    }

    private boolean isDeletable(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return false;
        }
        File file = new File(ROOT_DIRECTORY + filePath);
        return file.isFile();
    }
}
