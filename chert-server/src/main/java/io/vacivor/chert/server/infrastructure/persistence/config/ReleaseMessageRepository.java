package io.vacivor.chert.server.infrastructure.persistence.config;

import io.vacivor.chert.server.domain.config.ReleaseMessage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ReleaseMessageRepository extends JpaRepository<ReleaseMessage, Long> {

  ReleaseMessage findFirstByOrderByIdDesc();

  java.util.List<ReleaseMessage> findFirst100ByIdGreaterThanOrderByIdAsc(Long lastId);

  List<ReleaseMessage> findTop100ByAppIdAndEnvCodeAndNameInAndIdGreaterThanOrderByIdAsc(
      String appId, String envCode, Collection<String> names, Long lastId);

  long countByIdGreaterThan(Long lastId);

  @Modifying
  @Transactional
  @Query("delete from ReleaseMessage m where m.id < :id")
  void deleteByIdLessThan(Long id);
}
