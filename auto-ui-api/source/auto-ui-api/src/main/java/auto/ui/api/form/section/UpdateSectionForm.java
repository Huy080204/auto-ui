package auto.ui.api.form.section;

import auto.ui.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateSectionForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "projectData cannot be null")
    @Schema(name = "projectData", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectData;

    @NotNull(message = "isLock cannot be null")
    @Schema(name = "isLock", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isLock;
}
