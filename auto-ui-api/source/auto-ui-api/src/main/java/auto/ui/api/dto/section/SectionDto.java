package auto.ui.api.dto.section;

import auto.ui.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class SectionDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
    @Schema(name = "projectData")
    private String projectData;
    @Schema(name = "isLock")
    private Boolean isLock;
}
