package cz.be.r4u.repository;

import cz.be.r4u.entity.Roll;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.enums.RollStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RollRepository extends JpaRepository<Roll, Long> {

    @EntityGraph(attributePaths = "defects")
    @Query("""
            select distinct r
            from Roll r
            left join r.defects d
            where (:from is null or r.createdAt >= :from)
              and (:to is null or r.createdAt <= :to)
              and (:orderNumber is null or r.orderNumber = :orderNumber)
              and (:status is null or r.status = :status)
              and (:defectType is null or d.type = :defectType)
            order by r.createdAt desc
            """)
    List<Roll> findDashboardRows(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("orderNumber") Integer orderNumber,
            @Param("status") RollStatus status,
            @Param("defectType") DefectType defectType,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "defects")
    @Query("""
            select r
            from Roll r
            where r.id = :rollId
            """)
    Optional<Roll> findByIdWithDefects(@Param("rollId") Long rollId);
}