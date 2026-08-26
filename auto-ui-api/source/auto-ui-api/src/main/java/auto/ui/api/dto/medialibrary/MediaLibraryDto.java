package auto.ui.api.dto.medialibrary;

import auto.ui.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class MediaLibraryDto extends ABasicAdminDto {
    @Schema(name = "url")
    private String url;
}
