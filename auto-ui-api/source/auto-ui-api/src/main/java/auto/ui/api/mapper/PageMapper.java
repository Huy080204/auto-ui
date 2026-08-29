package auto.ui.api.mapper;

import auto.ui.api.dto.page.PageDto;
import auto.ui.api.form.page.AutoSavePageForm;
import auto.ui.api.form.page.CreatePageForm;
import auto.ui.api.form.page.UpdatePageForm;
import auto.ui.api.model.Pages;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {ABasicMapper.class})
public interface PageMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "projectData", target = "projectData")
    @Mapping(source = "isDraft", target = "isDraft")
    @Mapping(source = "activeVersion.id", target = "activeVersionId")
    @Mapping(source = "isDefault", target = "isDefault")
    @Mapping(source = "isHasDraft", target = "isHasDraft")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageDto")
    PageDto fromEntityToPageDto(Pages pages);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "isDraft", target = "isDraft")
    @Mapping(source = "activeVersion.id", target = "activeVersionId")
    @Mapping(source = "isDefault", target = "isDefault")
    @Mapping(source = "isHasDraft", target = "isHasDraft")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageListDto")
    PageDto fromEntityToPageListDto(Pages pages);

    @IterableMapping(elementTargetType = PageDto.class, qualifiedByName = "fromEntityToPageListDto")
    List<PageDto> fromEntityListToPageDtoList(List<Pages> pages);

    /** Biến thể cho /auto-complete — chỉ id + name + slug, đủ cho dropdown chọn trang. */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageAutoCompleteDto")
    PageDto fromEntityToPageAutoCompleteDto(Pages pages);

    @IterableMapping(elementTargetType = PageDto.class, qualifiedByName = "fromEntityToPageAutoCompleteDto")
    List<PageDto> fromEntityListToPageDtoAutoComplete(List<Pages> pages);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromFormToEntity")
    Pages fromFormToEntity(CreatePageForm createPageForm);

    /** Bản rút gọn cho endpoint public — Next.js chỉ cần tên trang và nội dung. */
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "projectData", target = "projectData")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPublicPageDto")
    PageDto fromEntityToPublicPageDto(Pages pages);

    /** Chỉ trả id — response chuẩn cho create theo itz-controller-conventions.md. */
    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageIdDto")
    PageDto fromEntityToPageIdDto(Pages pages);

    @Mapping(source = "projectData", target = "projectData", qualifiedByName = "jsonNodeToString")
    @BeanMapping(ignoreByDefault = true)
    void autoSaveEntityFromForm(AutoSavePageForm autoSavePageForm, @MappingTarget Pages pages);

    /** Chỉ name — slug bất biến sau khi tạo, xem javadoc UpdatePageForm. */
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    void updateEntityFromForm(UpdatePageForm updatePageForm, @MappingTarget Pages pages);

    /** Clone bản active thành draft mới — name/slug/projectData, phần còn lại Controller tự set. */
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "projectData", target = "projectData")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToDraft")
    Pages fromEntityToDraft(Pages pages);

    /** Merge nội dung draft lên row active khi promote — giữ nguyên id/slug/isDefault của active. */
    @Mapping(source = "name", target = "name")
    @Mapping(source = "projectData", target = "projectData")
    @BeanMapping(ignoreByDefault = true)
    void updateEntityFromDraft(Pages draft, @MappingTarget Pages activeVersion);
}
