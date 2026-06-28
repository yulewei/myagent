package ai.myagent.chat;

import ai.myagent.AgentApplication;
import ai.myagent.interceptor.LoggingClientInterceptor;
import ai.myagent.support.SqliteVectorStore;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
 * https://docs.spring.io/spring-ai/reference/api/embeddings/openai-embeddings.html
 * https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html
 *
 * @author yulewei
 * @since 2026/6/26
 */
@Slf4j
@SpringBootTest(classes = AgentApplication.class, properties = {"spring.profiles.active=dev"})
public class RagTest {
    @Value("${myagent.data-dir}")
    private String dataDir;
    @Value("${myagent.knowledge.vector-db-file:vectordb.json}")
    private String vectorDbFile;
    @Resource
    JdbcTemplate jdbcTemplate;
    EmbeddingModel embeddingModel;
    VectorStore vectorStore;
    ChatClient chatClient;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl("https://api.deepseek.com")
                        .apiKey(apiKey)
                        .restClientBuilder(RestClient.builder()
                                .requestInterceptor(new LoggingClientInterceptor()))
                        .build())
                .options(DeepSeekChatOptions.builder()
                        .model("deepseek-v4-flash")
                        .build())
                .build();
        chatClient = ChatClient.builder(chatModel).build();

        embeddingModel = OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder()
                        .apiKey(System.getenv("GLM_API_KEY"))
                        .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                        .model("embedding-3")
                        .dimensions(256)
                        .build())
                .build();
        vectorStore = SimpleVectorStore.builder(embeddingModel).build();

//        vectorStore = SqliteVectorStore.builder(jdbcTemplate, embeddingModel)
//                .dimensions(256)
//                .build();

//        vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
//                .dimensions(256)                    // Optional: defaults to model dimensions or 1536
//                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
//                .indexType(HNSW)                     // Optional: defaults to HNSW
//                .initializeSchema(true)              // Optional: defaults to false
//                .schemaName("public")                // Optional: defaults to "public"
//                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
//                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
//                .build();
    }

    @Test
    void test_embedding() {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of("Hello World", "World is big and salvation is near"));
        log.info("response: {}", JsonUtils.toJsonStr(response));
    }

    @Test
    void test_vectorStore() {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
        vectorStore.add(documents);
        vectorStore.save(new File("data/vector-store.json"));
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        log.info("results: {}", JsonUtils.toJsonStr(results));
    }

    @Test
    void test_parse_splitter() {
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource("README.md"));
        List<Document> documents = reader.get();
        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        List<Document> splitDocuments = splitter.apply(documents);
        log.info("分割成 {} 个 chunk，{}", splitDocuments.size(), JsonUtils.toJsonStr(splitDocuments));
//        vectorStore.add(splitDocuments);
    }

    @Test
    void test_similaritySearch() {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.load(new File(dataDir + "/" + vectorDbFile));

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("knowledgeId", "kb")
                .build();
        SearchRequest request = SearchRequest.builder()
                .query("MyAgent 项目简介？")
                .filterExpression(expression)
                .topK(3)
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("results: {}, {}", results.size(), JsonUtils.toJsonStr(results));
    }

    @Test
    void test_similaritySearch_with_filter() {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.load(new File(dataDir + "/" + vectorDbFile));

        Filter.Expression expression = new FilterExpressionBuilder().eq("knowledgeId", "kb").build();
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .filterExpression(expression)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
        String answer = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user("MyAgent 项目简介？")
                .call()
                .content();
        log.info("results: {}", answer);
    }
}
