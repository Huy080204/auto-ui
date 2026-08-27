package auto.ui.api.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ABasicMapper {

    @Named("jsonNodeToString")
    default String jsonNodeToString(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
