package cz.be.r4u.entity;

import cz.be.r4u.enums.DefectType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "defect")
@Getter
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DefectType type;

    @Column(nullable = false)
    private String defectLocation;

    @Column(nullable = false)
    private Double positionInRoll;

    @Column(nullable = false)
    private Double defectArea;

    @Column(nullable = false)
    private String defectDimension;

    @Column(nullable = false)
    private String classification;

    @Column(nullable = false)
    private String camera;

    @Column(nullable = false)
    private Integer bobinaColumnNumber;

    @Column(nullable = false, length = 500000)
    private byte[] image;

    @ManyToMany(mappedBy = "defects")
    private Set<Roll> rolls;
}
