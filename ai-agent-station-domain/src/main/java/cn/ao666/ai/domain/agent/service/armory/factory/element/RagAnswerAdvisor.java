package cn.ao666.ai.domain.agent.service.armory.factory.element;

import com.alibaba.fastjson.JSON;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagAnswerAdvisor implements BaseAdvisor {

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final String userTextAdvise;

/**
 * 构造函数：初始化RagAnswerAdvisor实例
 * @param vectorStore 向量存储，用于存储和检索向量数据
 * @param searchRequest 搜索请求，包含搜索的相关参数和信息
 */
    public RagAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
    // 初始化向量存储
        this.vectorStore = vectorStore;
    // 初始化搜索请求
        this.searchRequest = searchRequest;
    // 设置用户文本建议模板，用于生成回答时的上下文提示
        this.userTextAdvise = "\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n";
    }

/**
 * 在处理聊天客户端请求之前进行预处理的方法
 * @param chatClientRequest 原始的聊天客户端请求对象
 * @param advisorChain 顾问链对象，用于处理请求
 * @return 返回处理后的ChatClientRequest对象
 */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    // 创建一个新的HashMap来存储上下文信息
        HashMap<String, Object> context = new HashMap(chatClientRequest.context());

    // 获取用户输入的文本
        String userText = chatClientRequest.prompt().getUserMessage().getText();
    // 将用户文本与建议文本合并，作为新的用户输入
        String advisedUserText = userText + System.lineSeparator() + this.userTextAdvise;

    // 创建搜索请求，使用用户文本作为查询
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest).query(userText).filterExpression(this.doGetFilterExpression(context)).build();
    // 执行相似性搜索，获取相关文档
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
    // 将检索到的文档添加到上下文中
        context.put("qa_retrieved_documents", documents);

    // 将文档内容合并为一个字符串
        String documentContext = documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
    // 创建新的参数映射，包含问答上下文
        Map<String, Object> advisedUserParams = new HashMap(chatClientRequest.context());
        advisedUserParams.put("question_answer_context", documentContext);

    // 构建并返回处理后的聊天客户端请求
        return ChatClientRequest.builder()
                .prompt(Prompt.builder().messages(new UserMessage(advisedUserText), new AssistantMessage(JSON.toJSONString(advisedUserParams))).build())
                .context(advisedUserParams)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        chatResponseBuilder.metadata("qa_retrieved_documents", chatClientResponse.context().get("qa_retrieved_documents"));
        ChatResponse chatResponse = chatResponseBuilder.build();

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(this.before(chatClientRequest, callAdvisorChain));
        return this.after(chatClientResponse, callAdvisorChain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return BaseAdvisor.super.adviseStream(chatClientRequest, streamAdvisorChain);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString()) ? (new FilterExpressionTextParser()).parse(context.get("qa_filter_expression").toString()) : this.searchRequest.getFilterExpression();
    }

}
