package auto.ui.api.repository;

import auto.ui.api.model.Pages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Pages, Long>, JpaSpecificationExecutor<Pages> {
    Optional<Pages> findFirstBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByActiveVersionId(Long activeVersionId);

    Optional<Pages> findFirstByActiveVersionId(Long activeVersionId);

    @Modifying
    @Transactional
    @Query("UPDATE Pages p SET p.isDefault = false WHERE p.isDefault = true AND p.id <> :id")
    void unsetDefaultExcept(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM Pages p WHERE p.activeVersion.id = :activeVersionId")
    void deleteAllByActiveVersionId(@Param("activeVersionId") Long activeVersionId);
}
