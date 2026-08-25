package auto.ui.api.form.page;

import auto.ui.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Chỉ cho sửa name. Cố tình KHÔNG extends CreatePageForm dù itz-form-conventions.md
 * đặt đó làm mặc định: slug là khoá tra cứu của trang công khai (/public/get/{slug}),
 * đổi được là mọi liên kết đã phát ra ngoài chết theo — nên slug bất biến sau khi tạo.
 */
@Data
@Schema
public class UpdatePageForm {
    @NotNull(message = "id cant not be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
