package auto.ui.api.model.criteria;

import auto.ui.api.model.Pages;
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
public class PageCriteria implements Serializable {

    private Long id;
    private String name;
    private String slug;
    private Integer status;
    private Boolean isDraft;

    @Schema(hidden = true)
    public Specification<Pages> getCriteria() {
        return new Specification<Pages>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Pages> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (!StringUtils.isEmpty(getName())) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }

                if (!StringUtils.isEmpty(getSlug())) {
                    predicates.add(cb.like(cb.lower(root.get("slug")), "%" + getSlug().toLowerCase() + "%"));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }

                if (getIsDraft() != null) {
                    predicates.add(cb.equal(root.get("isDraft"), getIsDraft()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
