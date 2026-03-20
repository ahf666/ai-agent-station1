package cn.ao666.ai.domain.agent.model.valobj.enums;

import cn.ao666.ai.domain.agent.model.valobj.AiClientAdvisorVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import cn.ao666.ai.domain.agent.service.armory.factory.element.RagAnswerAdvisor;

import java.util.HashMap;
import java.util.Map;

/**
 * 顾问类型枚举类
 * 该枚举类定义了两种顾问类型：上下文记忆（内存模式）和知识库
 * 每种类型都有对应的创建顾问对象的方法
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AiClientAdvisorTypeEnumVO {

    /**
     * 上下文记忆（内存模式）类型的顾问
     * code: "ChatMemory"
     * info: "上下文记忆（内存模式）"
     */
    CHAT_MEMORY("ChatMemory", "上下文记忆（内存模式）") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            // 从配置对象中获取聊天记忆的配置
            AiClientAdvisorVO.ChatMemory chatMemory = aiClientAdvisorVO.getChatMemory();
            // 构建带有消息窗口聊天记忆的提示词顾问
            return PromptChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                            .maxMessages(chatMemory.getMaxMessages())  // 设置最大消息数
                            .build()
            ).build();
        }
    },
    
    /**
     * 知识库类型的顾问
     * code: "RagAnswer"
     * info: "知识库"
     */
    RAG_ANSWER("RagAnswer", "知识库") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            // 从配置对象中获取知识库答案的配置
            AiClientAdvisorVO.RagAnswer ragAnswer = aiClientAdvisorVO.getRagAnswer();
            // 构建知识库答案顾问
            return new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                    .topK(ragAnswer.getTopK())  // 设置返回结果数量
                    .filterExpression(ragAnswer.getFilterExpression())  // 设置过滤条件
                    .build());
        }
    }
    
    ;

    // 枚举的编码
    private String code;
    // 枚举的描述信息
    private String info;
    
    // 静态Map缓存，用于快速查找
    private static final Map<String, AiClientAdvisorTypeEnumVO> CODE_MAP = new HashMap<>();
    
    // 静态初始化块，在类加载时初始化Map
    static {
        for (AiClientAdvisorTypeEnumVO enumVO : values()) {
            CODE_MAP.put(enumVO.getCode(), enumVO);
        }
    }
    
    /**
     * 策略方法：创建顾问对象
     * @param aiClientAdvisorVO 顾问配置对象
     * @param vectorStore 向量存储
     * @return 顾问对象
     */
    public abstract Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore);
    
    /**
     * 根据code获取枚举
     * @param code 编码
     * @return 枚举对象
     */
    public static AiClientAdvisorTypeEnumVO getByCode(String code) {
        AiClientAdvisorTypeEnumVO enumVO = CODE_MAP.get(code);
        if (enumVO == null) {
            throw new RuntimeException("err! advisorType " + code + " not exist!");
        }
        return enumVO;
    }

}
