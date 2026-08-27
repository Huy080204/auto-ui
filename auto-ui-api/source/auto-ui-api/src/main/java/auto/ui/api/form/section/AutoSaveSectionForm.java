package auto.ui.api.form.section;

import auto.ui.api.form.StringToLongDeserializer;
import auto.ui.api.form.page.UpdatePageForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Schema
public class AutoSaveSectionForm {
    @NotNull(message = "id cant not be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "projectData cant not be null")
    @Schema(name = "projectData", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode projectData;
}
