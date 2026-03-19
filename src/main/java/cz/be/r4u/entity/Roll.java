package cz.be.r4u.entity;

import cz.be.r4u.enums.RollStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "roll")
@Getter
public class Roll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer orderNumber;

    @Column(nullable = false)
    private Integer min;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RollStatus status;

    @ManyToMany
    @JoinTable(
            name = "roll_defect",
            joinColumns = @JoinColumn(name = "roll_id"),
            inverseJoinColumns = @JoinColumn(name = "defect_id")
    )
    private Set<Defect> defects;
}
