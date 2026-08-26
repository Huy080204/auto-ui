package auto.ui.api.model;

import auto.ui.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Trang do admin dựng bằng GrapesJS.
 *
 * projectData: ProjectData của GrapesJS, chỉ editor đọc, sinh ra khi autosave — chuỗi opaque,
 * backend không parse.
 */
@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "page")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Page extends Auditable<String> {
    private String name;

    private String slug;

    @Column(name = "project_data", columnDefinition = "longtext")
    private String projectData;

    private Boolean isDraft = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_version_id")
    private Page activeVersion;

    private Boolean isDefault;

    private Boolean isHasDraft = false;
}
