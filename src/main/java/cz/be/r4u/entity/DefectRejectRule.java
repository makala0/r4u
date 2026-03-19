package cz.be.r4u.entity;

import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "defect_reject_rule")
@Getter
public class DefectRejectRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private DefectType defectType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DefectSizeClass rejectFromSizeClass;

    @Column(nullable = false)
    private boolean enabled;
}