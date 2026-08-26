package auto.ui.api.repository;

import auto.ui.api.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long>, JpaSpecificationExecutor<Page> {
    Optional<Page> findFirstBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByActiveVersionId(Long activeVersionId);

    Optional<Page> findFirstByActiveVersionId(Long activeVersionId);

    @Modifying
    @Transactional
    @Query("UPDATE Page p SET p.isDefault = false WHERE p.isDefault = true AND p.id <> :id")
    void unsetDefaultExcept(@Param("id") Long id);
}
