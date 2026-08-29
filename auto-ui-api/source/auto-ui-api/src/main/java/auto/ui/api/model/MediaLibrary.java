package auto.ui.api.model;

import auto.ui.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "media_library")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class MediaLibrary extends Auditable<String> {
    private String url;
    private Integer kind;
}
