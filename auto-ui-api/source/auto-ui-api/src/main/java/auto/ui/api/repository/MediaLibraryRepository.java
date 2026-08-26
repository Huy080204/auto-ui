package auto.ui.api.repository;

import auto.ui.api.model.MediaLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MediaLibraryRepository extends JpaRepository<MediaLibrary, Long>, JpaSpecificationExecutor<MediaLibrary> {
}
