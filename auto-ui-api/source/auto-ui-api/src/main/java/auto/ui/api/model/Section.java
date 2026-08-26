package auto.ui.api.model;

import auto.ui.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "section")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Section extends Auditable<String> {
    private String name;

    @Column(name = "project_data", columnDefinition = "LONGTEXT")
    private String projectData;

    private Boolean isLock;
}
