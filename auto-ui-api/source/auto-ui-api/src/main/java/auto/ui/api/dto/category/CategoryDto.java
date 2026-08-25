package auto.ui.api.dto.category;

import auto.ui.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class CategoryDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;

    @Schema(name = "description")
    private String description;

    @Schema(name = "avatar")
    private String avatar;
}
