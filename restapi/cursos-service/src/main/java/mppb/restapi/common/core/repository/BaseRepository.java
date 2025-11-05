package mppb.restapi.common.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BaseRepository<E, ID> extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {
}
