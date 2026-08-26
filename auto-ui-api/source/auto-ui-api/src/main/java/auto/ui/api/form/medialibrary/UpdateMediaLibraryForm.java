package auto.ui.api.form.medialibrary;

import auto.ui.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateMediaLibraryForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "file is required")
    @Schema(name = "file", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
