package ai.myagent.service.impl;

import ai.myagent.common.BizException;
import ai.myagent.constant.ProviderEnum;
import ai.myagent.interceptor.LoggingClientInterceptor;
import ai.myagent.interceptor.LoggingOkHttpInterceptor;
import ai.myagent.model.dto.AgentConfig;
import ai.myagent.model.dto.ChatModelDto;
import ai.myagent.model.dto.EmbeddingModelDto;
import ai.myagent.model.vo.ModelResp;
import ai.myagent.model.vo.SkillsVo;
import ai.myagent.service.ConfigService;
import ai.myagent.util.JsonUtils;
import ai.myagent.util.ScriptExecutor;
import ai.myagent.util.YamlUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.agent.utils.Skills;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 配置服务实现
 *
 * @author yulewei
 */
@Slf4j
@Service("configService")
public class ConfigServiceImpl extends FileAlterationListenerAdaptor implements ConfigService {
    public static final String INIT_DIR = "classpath:init/";

    @Value("${myagent.env.openai-api-key}")
    private String envOpenAiApiKey;
    @Value("${myagent.env.deepseek-api-key}")
    private String envDeepSeekApiKey;
    @Value("${myagent.env.glm-api-key}")
    private String envGlmApiKey;
    @Value("${myagent.env.qwen-api-key}")
    private String envQwenApiKey;

    @Value("${myagent.data-dir}")
    private String dataDir;
    @Value("${myagent.config-file}")
    private String configFileName;
    @Value("${myagent.db-file}")
    private String dbFileName;
    @Value("${myagent.vec0-install-file}")
    private String vec0InstallFile;
    @Value("#{'${myagent.skills-dir}'.split(',')}")
    private List<String> skillsDir;

    private AgentConfig.SessionConfig sessionDefault;

    private List<ChatModelDto> modelList;
    private EmbeddingModelDto embeddingModel;

    @Resource
    Environment env;

    @PostConstruct
    public void init() {
        // 初始化数据目录
        initDataDir();
        // 监控配置文件变更
        monitorConfigFile();
        // 加载配置文件
        boolean res = loadConfigFile();
        if (!res) {
            System.exit(1);
        }
    }

    /**
     * 初始化数据目录
     */
    @SneakyThrows
    private void initDataDir() {
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        log.info("初始化数据目录：{}", dataDir);
        File dbFile = new File(dataDir + dbFileName);
        if (!dbFile.exists()) {
            File file = ResourceUtils.getFile(INIT_DIR + dbFileName);
            FileUtils.copyFile(file, dbFile);
        }
        File installFile = new File(dataDir + vec0InstallFile);
        if (!installFile.exists()) {
            File file = ResourceUtils.getFile(INIT_DIR + vec0InstallFile);
            FileUtils.copyFile(file, installFile);

            ScriptExecutor.Result result = ScriptExecutor.run(dataDir + vec0InstallFile);
            if (result.isSuccess()) {
                log.info("sqlite 扩展 vec0 下载成功");
            } else {
                log.error("sqlite 扩展 vec0 下载失败：{}", result.getError());
                System.exit(1);
            }
        }

        File configFile = new File(dataDir + configFileName);
        if (!configFile.exists()) {
            File file = ResourceUtils.getFile(INIT_DIR + configFile);
            FileUtils.copyFile(file, configFile);
        }
    }

    /**
     * 监控配置文件变更
     */
    @SneakyThrows
    private void monitorConfigFile() {
        IOFileFilter filter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.nameFileFilter(FilenameUtils.getName(configFileName)));
        FileAlterationObserver observer = new FileAlterationObserver(new File(dataDir), filter);
        // 每隔 1000 毫秒扫描一次
        FileAlterationMonitor monitor = new FileAlterationMonitor(1000L);
        observer.addListener(this);
        monitor.addObserver(observer);
        monitor.start();
    }

    @Override
    public void onFileChange(final File file) {
        log.info("Config file changed: {}", file.getAbsolutePath());
        loadConfigFile();
    }

    /**
     * 加载配置文件
     */
    @SneakyThrows
    private boolean loadConfigFile() {
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        String configFileName = dataDir + this.configFileName;
        String content = FileUtils.readFileToString(new File(configFileName), StandardCharsets.UTF_8);
        AgentConfig config = YamlUtils.parse(content, AgentConfig.class);
        if (config == null || config.getDefaultConfig() == null
                || config.getDefaultConfig().getSession() == null
                || config.getDefaultConfig().getSession().getModelId() == null) {
            log.error("加载配置文件：{}，未配置默认模型", configFileName);
            return false;
        }
        modelList = initChatModels(config);
        List<String> modelNames = modelList.stream()
                .map(e -> e.getProviderName() + ": " + e.getModelId()).toList();
        log.info("加载配置文件 {} ：全部可用模型：{}", configFileName, JsonUtils.toJsonStr(modelNames));
        ChatModelDto defaultModel = modelList.stream()
                .filter(ChatModelDto::getIsDefault)
                .findFirst().orElse(null);
        if (defaultModel == null) {
            log.error("加载配置文件 {} ：默认模型 {}，未正确配置", configFileName, config.getDefaultConfig().getSession().getModelId());
            return false;
        }
        sessionDefault = config.getDefaultConfig().getSession();
        log.info("加载配置文件 {} ：默认模型：{}", configFileName, config.getDefaultConfig().getSession().getModelId());
        embeddingModel = initEmbeddingModel(config);
        if (embeddingModel != null) {
            log.info("加载配置文件 {} ：默认向量化模型 {}", configFileName, config.getDefaultConfig().getEmbedding().getModelId());
        }
        return true;
    }


    private List<ChatModelDto> initChatModels(AgentConfig config) {
        AgentConfig.SessionConfig sessionDefault = config.getDefaultConfig().getSession();
        List<AgentConfig.ProviderConfig> providers = config.getProviders();
        List<ChatModelDto> list = new ArrayList<>();
        for (AgentConfig.ProviderConfig p : providers) {
            if (Objects.equals(p.getEnabled(), false)) {
                continue;
            }
            for (AgentConfig.ModelInfo model : p.getModels()) {
                ChatModelDto dto = initChatModel(sessionDefault, p, model);
                if (dto != null) {
                    list.add(dto);
                }
            }
        }
        return list;
    }

    private ChatModelDto initChatModel(AgentConfig.SessionConfig sessionDefault,
                                       AgentConfig.ProviderConfig p, AgentConfig.ModelInfo model) {
        ProviderEnum provider = ProviderEnum.of(p.getId());
        if (provider == null) {
            return null;
        }
        String apiKey = this.resolveApiKey(provider, p.getApiKey());
        ChatModel chatModel;
        if (provider != ProviderEnum.DEEPSEEK) {
            chatModel = OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .baseUrl(p.getBaseUrl())
                            .apiKey(apiKey)
                            .model(model.getId())
                            .temperature(sessionDefault.getTemperature())
                            .build())
                    .httpClientBuilderCustomizer(builder -> {
                        builder.interceptor(new LoggingOkHttpInterceptor());
                    })
                    .build();
        } else {
            chatModel = DeepSeekChatModel.builder()
                    .deepSeekApi(DeepSeekApi.builder()
                            .baseUrl(p.getBaseUrl())
                            .apiKey(apiKey)
                            .restClientBuilder(RestClient.builder()
                                    .requestInterceptor(new LoggingClientInterceptor()))
                            .build())
                    .options(DeepSeekChatOptions.builder()
                            .model(model.getId())
                            .temperature(sessionDefault.getTemperature())
                            .build())
                    .build();
        }
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return ChatModelDto.builder()
                .isDefault(model.getId().equals(sessionDefault.getModelId()))
                .providerId(p.getId())
                .providerName(p.getName())
                .modelId(model.getId())
                .baseUrl(p.getBaseUrl())
                .apiKey(apiKey)
                .chatModel(chatModel)
                .chatClient(chatClient)
                .build();
    }

    private EmbeddingModelDto initEmbeddingModel(AgentConfig config) {
        AgentConfig.EmbeddingConfig embeddingDefault = config.getDefaultConfig().getEmbedding();
        ProviderEnum provider = ProviderEnum.of(embeddingDefault.getProviderId());
        if (provider == null) {
            return null;
        }
        AgentConfig.ProviderConfig p = config.getProviders().stream()
                .filter(e -> e.getId().equals(embeddingDefault.getProviderId()))
                .findFirst().orElse(null);
        if (p == null) {
            return null;
        }
        AgentConfig.ModelInfo model = p.getModels().stream()
                .filter(e -> e.getId().equals(embeddingDefault.getModelId()))
                .findFirst().orElse(null);
        if (model == null) {
            return null;
        }
        String apiKey = this.resolveApiKey(provider, p.getApiKey());
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.EMBED)
                .options(OpenAiEmbeddingOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(p.getBaseUrl())
                        .model(embeddingDefault.getModelId())
                        .dimensions(embeddingDefault.getDimensions())
                        .build())
                .build();
        return EmbeddingModelDto.builder()
                .providerId(p.getId())
                .providerName(p.getName())
                .modelId(model.getId())
                .baseUrl(p.getBaseUrl())
                .apiKey(apiKey)
                .embeddingModel(embeddingModel)
                .build();
    }

    @Override
    public void reloadConfig() {
        loadConfigFile();
    }

    /**
     * 修改默认模型
     */
    @Override
    @SneakyThrows
    public void switchDefaultModel(String modelId) {
        if (sessionDefault.getModelId().equals(modelId)) {
            return;
        }
        ChatModelDto defaultModel = modelList.stream()
                .filter(m -> m.getModelId().equals(modelId))
                .findFirst().orElse(null);
        if (defaultModel == null) {
            throw new BizException("模型 {} 不存在", modelId);
        }
        for (ChatModelDto dto : modelList) {
            dto.setIsDefault(dto.getModelId().equals(modelId));
        }
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        String configFileName = dataDir + this.configFileName;
        String content = FileUtils.readFileToString(new File(configFileName), StandardCharsets.UTF_8);
        AgentConfig config = YamlUtils.parse(content, AgentConfig.class);
        config.getDefaultConfig().getSession().setModelId(modelId);
        FileUtils.writeStringToFile(new File(configFileName), YamlUtils.toYaml(config), StandardCharsets.UTF_8);
    }

    @Override
    public AgentConfig.SessionConfig querySessionDefault() {
        return sessionDefault;
    }

    @Override
    public ChatModelDto queryModel(String modelId) {
        return modelList.stream().filter(m -> m.getModelId().equals(modelId))
                .findFirst().orElse(null);
    }

    @Override
    public List<ChatModelDto> queryModelList() {
        return modelList;
    }

    @Override
    public List<ModelResp> queryModelVoList() {
        return modelList.stream().map(e -> ModelResp.builder()
                .isDefault(e.getIsDefault())
                .providerId(e.getProviderId())
                .providerName(e.getProviderName())
                .modelId(e.getModelId())
                .baseUrl(e.getBaseUrl())
                .apiKey(e.getApiKey())
                .build()).toList();
    }

    @Override
    public List<SkillsVo> querySkillList() {
        List<String> dirList = skillsDir.stream().map(String::trim)
                .distinct()
                .filter(StringUtils::isNotBlank)
                .filter(e -> new File(e).exists())
                .toList();
        List<SkillsVo> list = new ArrayList<>();
        for (String dir : dirList) {
            List<SkillsVo.SkillVo> skills = Skills.loadDirectory(dir).stream()
                    .map(e -> SkillsVo.SkillVo.builder()
                            .name(e.name())
                            .description(e.frontMatter().get("description").toString())
                            .build())
                    .toList();
            list.add(SkillsVo.builder().dir(dir).skills(skills).build());
        }
        return list;
    }

    @Override
    public EmbeddingModelDto queryEmbeddingModel() {
        return embeddingModel;
    }

    private String resolveApiKey(ProviderEnum provider, String apiKey) {
        String resolvedApiKey = env.resolvePlaceholders(apiKey);
        // 若环境变量中存在 api_key 配置，则优先使用环境变量中的值，覆盖配置文件中的值
        apiKey = switch (provider) {
            case OPENAI -> ObjectUtils.getIfNull(System.getenv(envOpenAiApiKey), apiKey);
            case DEEPSEEK -> ObjectUtils.getIfNull(System.getenv(envDeepSeekApiKey), apiKey);
            case GLM -> ObjectUtils.getIfNull(System.getenv(envGlmApiKey), apiKey);
            case QWEN -> ObjectUtils.getIfNull(System.getenv(envQwenApiKey), apiKey);
        };
        return resolvedApiKey;
    }
}
