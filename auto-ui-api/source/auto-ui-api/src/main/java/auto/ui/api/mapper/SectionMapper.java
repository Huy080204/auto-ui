package auto.ui.api.mapper;

import auto.ui.api.dto.section.SectionDto;
import auto.ui.api.form.section.CreateSectionForm;
import auto.ui.api.form.section.UpdateSectionForm;
import auto.ui.api.model.Section;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SectionMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "projectData", target = "projectData")
    @Mapping(source = "isLock", target = "isLock")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToSectionDto")
    SectionDto fromEntityToSectionDto(Section section);

    @IterableMapping(elementTargetType = SectionDto.class, qualifiedByName = "fromEntityToSectionDto")
    List<SectionDto> fromEntityToSectionDtoList(List<Section> sections);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "projectData", target = "projectData")
    @Mapping(source = "isLock", target = "isLock")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromFormToEntity")
    Section fromFormToEntity(CreateSectionForm createSectionForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "projectData", target = "projectData")
    @Mapping(source = "isLock", target = "isLock")
    @BeanMapping(ignoreByDefault = true)
    @Named("updateEntityFromForm")
    void updateEntityFromForm(UpdateSectionForm updateSectionForm, @MappingTarget Section section);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToSectionIdDto")
    SectionDto fromEntityToSectionIdDto(Section section);
}
