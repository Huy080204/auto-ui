package auto.ui.api.form.medialibrary;

import auto.ui.api.validation.MediaLibraryKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class CreateMediaLibraryForm {
    @NotNull(message = "file is required")
    @Schema(name = "file", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

    @NotNull(message = "kind is required")
    @MediaLibraryKind
    @Schema(name = "kind", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer kind;
}
