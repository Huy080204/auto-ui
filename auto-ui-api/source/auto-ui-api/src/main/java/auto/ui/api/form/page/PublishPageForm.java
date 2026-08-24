package auto.ui.api.form.page;

import auto.ui.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Schema
public class PublishPageForm {
    @NotNull(message = "id cant not be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /** { blocks: [{ type, props }] } — chỉ `type` được validate, `props` giữ nguyên. */
    @NotNull(message = "config cant not be null")
    @Schema(name = "config", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode config;
}
