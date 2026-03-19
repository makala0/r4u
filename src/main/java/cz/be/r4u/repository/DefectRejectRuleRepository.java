package cz.be.r4u.repository;

import cz.be.r4u.entity.DefectRejectRule;
import cz.be.r4u.enums.DefectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DefectRejectRuleRepository extends JpaRepository<DefectRejectRule, Long> {

    Optional<DefectRejectRule> findByDefectType(DefectType defectType);
}