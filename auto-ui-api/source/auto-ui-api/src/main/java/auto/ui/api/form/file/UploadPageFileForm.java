package auto.ui.api.form.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Schema
public class UploadPageFileForm {
    @NotNull(message = "pageId is required")
    @Schema(name = "pageId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long pageId;

    @NotNull(message = "file is required")
    @Schema(name = "file", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
