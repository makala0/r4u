package cz.be.r4u.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "defect_size_threshold_config")
@Getter
public class DefectSizeThresholdConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double sMaxArea;

    @Column(nullable = false)
    private Double mMaxArea;

    @Column(nullable = false)
    private Double lMaxArea;
}