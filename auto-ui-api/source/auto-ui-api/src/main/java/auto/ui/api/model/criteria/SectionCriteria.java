package auto.ui.api.model.criteria;

import auto.ui.api.model.Section;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SectionCriteria implements Serializable {

    private Long id;
    private String name;
    private Boolean isLock;
    private Integer status;

    @Schema(hidden = true)
    public Specification<Section> getCriteria() {
        return new Specification<Section>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<Section> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (!StringUtils.isEmpty(getName())) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }

                if (getIsLock() != null) {
                    predicates.add(cb.equal(root.get("isLock"), getIsLock()));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
