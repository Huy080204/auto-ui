package auto.ui.api.form.page;

import auto.ui.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * Input của /autosave — không phải form update của bộ CRUD chuẩn, nên tách tên riêng:
 * nó chỉ ghi một cột opaque. Sửa name/slug là việc của {@link UpdatePageForm}.
 */
@Getter
@Setter
@Schema
public class AutoSavePageForm {
    @NotNull(message = "id cant not be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /** ProjectData của GrapesJS — nhận nguyên cây JSON, backend không đọc nội dung. */
    @NotNull(message = "projectData cant not be null")
    @Schema(name = "projectData", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode projectData;
}
