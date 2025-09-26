package mppb.restapi.common.core.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity<ID> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private ID id;

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AbstractEntity)){
            return false;
        }
        AbstractEntity another = (AbstractEntity) obj;
        if(getId() == null || another.getId() == null){
            return false;
        }
        return getId().equals(another.getId());
    }

    @Override
    public int hashCode() {
        if(getId() != null){
            return getId().hashCode();
        }
        return 0;
    }
}
