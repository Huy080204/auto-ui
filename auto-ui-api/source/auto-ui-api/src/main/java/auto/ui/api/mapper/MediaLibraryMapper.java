package auto.ui.api.mapper;

import auto.ui.api.dto.medialibrary.MediaLibraryDto;
import auto.ui.api.model.MediaLibrary;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MediaLibraryMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "url", target = "url")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToMediaLibraryDto")
    MediaLibraryDto fromEntityToMediaLibraryDto(MediaLibrary mediaLibrary);

    @IterableMapping(elementTargetType = MediaLibraryDto.class, qualifiedByName = "fromEntityToMediaLibraryDto")
    List<MediaLibraryDto> fromEntityToMediaLibraryDtoList(List<MediaLibrary> mediaLibraries);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToMediaLibraryIdDto")
    MediaLibraryDto fromEntityToMediaLibraryIdDto(MediaLibrary mediaLibrary);
}
