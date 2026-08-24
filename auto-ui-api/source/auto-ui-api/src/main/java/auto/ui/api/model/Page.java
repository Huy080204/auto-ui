package auto.ui.api.model;

import auto.ui.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Date;

/**
 * Trang do admin dựng bằng GrapesJS.
 *
 * Hai cột JSON được lưu dưới dạng chuỗi opaque — backend không parse, nhận sao lưu vậy:
 * - projectData: ProjectData của GrapesJS, chỉ editor đọc, sinh ra khi autosave.
 * - pageConfig:  schema tự định nghĩa { blocks: [{ type, props }] }, Next.js đọc để render,
 *   sinh ra khi publish. Ngoại lệ duy nhất backend đụng vào là validate `type` lúc publish.
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

    @Column(name = "page_config", columnDefinition = "longtext")
    private String pageConfig;

    /** Optimistic lock cho autosave — editor gửi kèm version nó đang giữ. */
    @Version
    private Long version;

    @Column(name = "published_at")
    private Date publishedAt;
}
