package cz.be.r4u.service;

import cz.be.r4u.entity.Defect;
import cz.be.r4u.entity.Roll;
import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.enums.RollStatus;
import cz.be.r4u.repository.DefectRepository;
import cz.be.r4u.repository.RollRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private static final int MAX_ROWS = 50;

    private final RollRepository rollRepository;
    private final DefectRepository defectRepository;
    private final DefectRejectRuleService defectRejectRuleService;

    public DashboardService(
            RollRepository rollRepository,
            DefectRepository defectRepository,
            DefectRejectRuleService defectRejectRuleService
    ) {
        this.rollRepository = rollRepository;
        this.defectRepository = defectRepository;
        this.defectRejectRuleService = defectRejectRuleService;
    }

    public DashboardData loadInitialDashboard() {
        return filterDashboard(new DashboardFilter(null, null, null, null, null));
    }

    public DashboardData filterDashboard(DashboardFilter filter) {
        List<Roll> rolls = rollRepository.findDashboardRows(
                filter.from(),
                filter.to(),
                filter.orderNumber(),
                filter.status(),
                filter.defectType(),
                PageRequest.of(0, MAX_ROWS)
        );

        List<DashboardRow> rows = rolls.stream()
                .map(this::toRow)
                .toList();

        long totalCount = rolls.size();
        long okCount = rolls.stream()
                .filter(roll -> roll.getStatus() == RollStatus.OK)
                .count();

        long nokCount = rolls.stream()
                .filter(roll -> roll.getStatus() == RollStatus.NOK)
                .count();

        long defectRollCount = rolls.stream()
                .filter(roll -> roll.getDefects() != null && !roll.getDefects().isEmpty())
                .count();

        DashboardSummary summary = new DashboardSummary(
                totalCount,
                okCount,
                nokCount,
                defectRollCount
        );

        return new DashboardData(rows, summary);
    }

    public DashboardRow loadDashboardRowByRollId(Long rollId) {
        if (rollId == null) {
            throw new IllegalArgumentException("ID role nesmí být null.");
        }

        Roll roll = rollRepository.findByIdWithDefects(rollId)
                .orElseThrow(() -> new IllegalArgumentException("Role nebyla nalezena."));

        return toRow(roll);
    }

    public byte[] loadDefectImage(Long defectId) {
        if (defectId == null) {
            return null;
        }

        return defectRepository.findById(defectId)
                .map(Defect::getImage)
                .orElse(null);
    }

    private DashboardRow toRow(Roll roll) {
        List<DefectRow> defectRows = roll.getDefects() == null
                ? List.of()
                : roll.getDefects().stream()
                .sorted(Comparator.comparing(Defect::getCreatedAt))
                .map(this::toDefectRow)
                .toList();

        return new DashboardRow(
                roll.getId(),
                roll.getCreatedAt(),
                roll.getOrderNumber(),
                roll.getMin(),
                roll.getStatus(),
                defectRows.size(),
                defectRows
        );
    }

    private DefectRow toDefectRow(Defect defect) {
        DefectSizeClass sizeClass = defectRejectRuleService.resolveSizeClass(defect.getDefectArea());
        boolean reject = defectRejectRuleService.isReject(defect.getType(), defect.getDefectArea());

        return new DefectRow(
                defect.getId(),
                defect.getCreatedAt(),
                defect.getType(),
                defect.getCamera(),
                defect.getPositionInRoll(),
                defect.getDefectArea(),
                defect.getDefectLocation(),
                defect.getClassification(),
                defect.getBobinaColumnNumber(),
                defect.getImage() != null && defect.getImage().length > 0,
                sizeClass,
                reject
        );
    }

    public record DashboardFilter(
            LocalDateTime from,
            LocalDateTime to,
            Integer orderNumber,
            RollStatus status,
            DefectType defectType
    ) {
    }

    public record DashboardData(
            List<DashboardRow> rows,
            DashboardSummary summary
    ) {
    }

    public record DashboardSummary(
            long totalCount,
            long okCount,
            long nokCount,
            long defectRollCount
    ) {
    }

    public record DashboardRow(
            Long rollId,
            LocalDateTime createdAt,
            Integer orderNumber,
            Integer min,
            RollStatus status,
            Integer defectsCount,
            List<DefectRow> defects
    ) {
    }

    public record DefectRow(
            Long defectId,
            LocalDateTime createdAt,
            DefectType type,
            String camera,
            Double positionInRoll,
            Double defectArea,
            String defectLocation,
            String classification,
            Integer bobinaColumnNumber,
            boolean hasImage,
            DefectSizeClass sizeClass,
            boolean reject
    ) {
    }
}