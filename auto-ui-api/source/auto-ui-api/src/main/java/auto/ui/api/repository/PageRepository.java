package auto.ui.api.repository;

import auto.ui.api.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long>, JpaSpecificationExecutor<Page> {
    Optional<Page> findFirstBySlug(String slug);

    boolean existsBySlug(String slug);
}
