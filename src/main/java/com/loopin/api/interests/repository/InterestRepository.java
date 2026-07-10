package com.loopin.api.interests.repository;

import com.loopin.api.interests.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterestRepository extends JpaRepository<Interest, Long> {

    Optional<Interest> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    Optional<Interest> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlug(String slug);

    List<Interest> findAllByDeletedAtIsNullOrderByCategoryAscNameAsc();

    List<Interest> findByPublicIdInAndDeletedAtIsNull(Collection<UUID> publicIds);
}
