package cz.be.r4u.service;

import cz.be.r4u.entity.DefectRejectRule;
import cz.be.r4u.entity.DefectSizeThresholdConfig;
import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.repository.DefectRejectRuleRepository;
import cz.be.r4u.repository.DefectSizeThresholdConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DefectRejectRuleService {

    private static final double DEFAULT_S_MAX_AREA = 5.0;
    private static final double DEFAULT_M_MAX_AREA = 10.0;
    private static final double DEFAULT_L_MAX_AREA = 15.0;

    private final DefectRejectRuleRepository defectRejectRuleRepository;
    private final DefectSizeThresholdConfigRepository thresholdConfigRepository;

    public DefectRejectRuleService(
            DefectRejectRuleRepository defectRejectRuleRepository,
            DefectSizeThresholdConfigRepository thresholdConfigRepository
    ) {
        this.defectRejectRuleRepository = defectRejectRuleRepository;
        this.thresholdConfigRepository = thresholdConfigRepository;
    }

    public DefectSizeClass resolveSizeClass(Double defectArea) {
        if (defectArea == null) {
            return null;
        }

        SizeThresholds thresholds = getThresholds();

        if (defectArea <= thresholds.sMaxArea()) {
            return DefectSizeClass.S;
        }

        if (defectArea <= thresholds.mMaxArea()) {
            return DefectSizeClass.M;
        }

        if (defectArea <= thresholds.lMaxArea()) {
            return DefectSizeClass.L;
        }

        return DefectSizeClass.XL;
    }

    public boolean isReject(DefectType defectType, Double defectArea) {
        if (defectType == null || defectArea == null) {
            return false;
        }

        DefectSizeClass actualSizeClass = resolveSizeClass(defectArea);

        return defectRejectRuleRepository.findByDefectType(defectType)
                .filter(DefectRejectRule::isEnabled)
                .map(DefectRejectRule::getRejectFromSizeClass)
                .map(rejectFrom -> actualSizeClass.ordinal() >= rejectFrom.ordinal())
                .orElse(false);
    }

    @Transactional
    public List<DefectRejectRule> getOrCreateAllRules() {
        Map<DefectType, DefectRejectRule> existingRules = new EnumMap<>(DefectType.class);

        defectRejectRuleRepository.findAll().forEach(rule -> existingRules.put(rule.getDefectType(), rule));

        for (DefectType defectType : DefectType.values()) {
            if (!existingRules.containsKey(defectType)) {
                DefectRejectRule rule = new DefectRejectRule();
                setRuleValues(rule, defectType, DefectSizeClass.L, true);
                defectRejectRuleRepository.save(rule);
                existingRules.put(defectType, rule);
            }
        }

        return defectRejectRuleRepository.findAll().stream()
                .sorted((left, right) -> left.getDefectType().name().compareTo(right.getDefectType().name()))
                .toList();
    }

    @Transactional
    public void updateRule(Long ruleId, DefectSizeClass rejectFromSizeClass, boolean enabled) {
        DefectRejectRule rule = defectRejectRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Pravidlo pro vyřazení nebylo nalezeno."));

        setRuleValues(rule, rule.getDefectType(), rejectFromSizeClass, enabled);
        defectRejectRuleRepository.save(rule);
    }

    @Transactional
    public SizeThresholds getThresholds() {
        DefectSizeThresholdConfig config = getOrCreateThresholdConfig();

        return new SizeThresholds(
                config.getSMaxArea(),
                config.getMMaxArea(),
                config.getLMaxArea()
        );
    }

    @Transactional
    public void updateThresholds(Double sMaxArea, Double mMaxArea, Double lMaxArea) {
        validateThresholds(sMaxArea, mMaxArea, lMaxArea);

        DefectSizeThresholdConfig config = getOrCreateThresholdConfig();
        setThresholdValues(config, sMaxArea, mMaxArea, lMaxArea);
        thresholdConfigRepository.save(config);
    }

    private DefectSizeThresholdConfig getOrCreateThresholdConfig() {
        return thresholdConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    DefectSizeThresholdConfig config = new DefectSizeThresholdConfig();
                    setThresholdValues(config, DEFAULT_S_MAX_AREA, DEFAULT_M_MAX_AREA, DEFAULT_L_MAX_AREA);
                    return thresholdConfigRepository.save(config);
                });
    }

    private void validateThresholds(Double sMaxArea, Double mMaxArea, Double lMaxArea) {
        if (sMaxArea == null || mMaxArea == null || lMaxArea == null) {
            throw new IllegalArgumentException("Všechny hranice velikostí musí být vyplněné.");
        }

        if (sMaxArea <= 0 || mMaxArea <= 0 || lMaxArea <= 0) {
            throw new IllegalArgumentException("Hranice velikostí musí být větší než 0.");
        }

        if (!(sMaxArea < mMaxArea && mMaxArea < lMaxArea)) {
            throw new IllegalArgumentException("Musí platit S < M < L.");
        }
    }

    private void setRuleValues(
            DefectRejectRule rule,
            DefectType defectType,
            DefectSizeClass rejectFromSizeClass,
            boolean enabled
    ) {
        try {
            var defectTypeField = DefectRejectRule.class.getDeclaredField("defectType");
            defectTypeField.setAccessible(true);
            defectTypeField.set(rule, defectType);

            var rejectFromField = DefectRejectRule.class.getDeclaredField("rejectFromSizeClass");
            rejectFromField.setAccessible(true);
            rejectFromField.set(rule, rejectFromSizeClass);

            var enabledField = DefectRejectRule.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.set(rule, enabled);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nepodařilo se nastavit hodnoty pravidla.", e);
        }
    }

    private void setThresholdValues(
            DefectSizeThresholdConfig config,
            Double sMaxArea,
            Double mMaxArea,
            Double lMaxArea
    ) {
        try {
            var sField = DefectSizeThresholdConfig.class.getDeclaredField("sMaxArea");
            sField.setAccessible(true);
            sField.set(config, sMaxArea);

            var mField = DefectSizeThresholdConfig.class.getDeclaredField("mMaxArea");
            mField.setAccessible(true);
            mField.set(config, mMaxArea);

            var lField = DefectSizeThresholdConfig.class.getDeclaredField("lMaxArea");
            lField.setAccessible(true);
            lField.set(config, lMaxArea);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nepodařilo se nastavit hranice velikostí.", e);
        }
    }

    public record SizeThresholds(
            Double sMaxArea,
            Double mMaxArea,
            Double lMaxArea
    ) {
    }
}