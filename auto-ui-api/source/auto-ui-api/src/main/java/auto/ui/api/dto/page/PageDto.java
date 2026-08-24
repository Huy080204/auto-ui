package auto.ui.api.dto.page;

import auto.ui.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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

    @Schema(name = "pageConfig")
    private String pageConfig;

    @Schema(name = "version")
    private Long version;

    @Schema(name = "publishedAt")
    private Date publishedAt;
}
