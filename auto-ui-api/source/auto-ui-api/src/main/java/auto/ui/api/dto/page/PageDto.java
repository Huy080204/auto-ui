package auto.ui.api.dto.page;

import auto.ui.api.dto.ABasicAdminDto;
import auto.ui.api.dto.LongToStringIfWebSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class PageDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;

    @Schema(name = "slug")
    private String slug;

    @Schema(name = "projectData")
    private String projectData;

    @Schema(name = "isDraft")
    private Boolean isDraft;

    @Schema(name = "activeVersionId")
    @JsonSerialize(using = LongToStringIfWebSerializer.class)
    private Long activeVersionId;

    @Schema(name = "isDefault")
    private Boolean isDefault;
}
