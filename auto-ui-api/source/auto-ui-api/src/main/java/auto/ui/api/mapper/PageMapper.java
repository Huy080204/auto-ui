package auto.ui.api.mapper;

import auto.ui.api.dto.page.PageDto;
import auto.ui.api.form.page.AutoSavePageForm;
import auto.ui.api.form.page.CreatePageForm;
import auto.ui.api.form.page.PublishPageForm;
import auto.ui.api.form.page.UpdatePageForm;
import auto.ui.api.model.Page;
import com.fasterxml.jackson.databind.JsonNode;
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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PageMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "projectData", target = "projectData")
    @Mapping(source = "pageConfig", target = "pageConfig")
    @Mapping(source = "version", target = "version")
    @Mapping(source = "publishedAt", target = "publishedAt")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageDto")
    PageDto fromEntityToPageDto(Page page);

    @IterableMapping(elementTargetType = PageDto.class, qualifiedByName = "fromEntityToPageDto")
    List<PageDto> fromEntityListToPageDtoList(List<Page> pages);

    /** Biến thể cho /auto-complete — chỉ id + name + slug, đủ cho dropdown chọn trang. */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageAutoCompleteDto")
    PageDto fromEntityToPageAutoCompleteDto(Page page);

    @IterableMapping(elementTargetType = PageDto.class, qualifiedByName = "fromEntityToPageAutoCompleteDto")
    List<PageDto> fromEntityListToPageDtoAutoComplete(List<Page> pages);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromFormToEntity")
    Page fromFormToEntity(CreatePageForm createPageForm);

    /** Bản rút gọn cho endpoint public — Next.js chỉ cần tên trang, config và mốc publish. */
    @Mapping(source = "name", target = "name")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "pageConfig", target = "pageConfig")
    @Mapping(source = "publishedAt", target = "publishedAt")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPublicPageDto")
    PageDto fromEntityToPublicPageDto(Page page);

    /** Chỉ trả id + version — đủ để editor cập nhật optimistic lock sau autosave. */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "version", target = "version")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToPageIdDto")
    PageDto fromEntityToPageIdDto(Page page);

    @Mapping(source = "projectData", target = "projectData")
    @BeanMapping(ignoreByDefault = true)
    void autoSaveEntityFromForm(AutoSavePageForm autoSavePageForm, @MappingTarget Page page);

    /** Chỉ name — slug bất biến sau khi tạo, xem javadoc UpdatePageForm. */
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    void updateEntityFromForm(UpdatePageForm updatePageForm, @MappingTarget Page page);

    @Mapping(source = "config", target = "pageConfig")
    @BeanMapping(ignoreByDefault = true)
    void publishEntityFromForm(PublishPageForm publishPageForm, @MappingTarget Page page);

    /**
     * Hai cột JSON lưu dạng chuỗi opaque, còn Form nhận nguyên cây JSON —
     * conversion nằm ở Mapper để Controller không phải setter tay.
     */
    default String jsonNodeToString(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
