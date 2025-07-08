package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * SOC有效性检查规则
 * 检查SOC值是否在有效范围内
 */
@RuleDefinition(
    type = "SOC_VALIDITY",
    code = 1002,
    description = "SOC无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class SocValidityRule extends AbstractRule {
    
    // SOC的有效范围
    private static final int MIN_SOC = 0;
    private static final int MAX_SOC = 100;
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer soc = data.getSoc();
        
        // 如果SOC为空，不进行检查
        if (soc == null) {
            return noIssue();
        }
        
        // SOC取值范围: [0, 100]
        if (soc < MIN_SOC || soc > MAX_SOC) {
            return singleIssue(data, 
                    String.format("SOC: %d 超出有效范围[%d, %d]", soc, MIN_SOC, MAX_SOC));
        }
        
        return noIssue();
    }
} 
