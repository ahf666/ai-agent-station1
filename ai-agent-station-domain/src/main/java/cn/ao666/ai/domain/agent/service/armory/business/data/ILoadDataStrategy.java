package cn.ao666.ai.domain.agent.service.armory.business.data;

import cn.ao666.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.ao666.ai.domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;

/**
 * 数据加载策略
 *
 */
public interface ILoadDataStrategy {

    void loadData(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext);

}
