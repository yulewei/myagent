package ai.myagent.constant;

import ai.myagent.tool.WeatherTools;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springaicommunity.agent.tools.*;

import java.util.List;

/**
 * 内置工具
 *
 * @author yulewei
 * @since 2026/6/22
 */
public interface Tools {

    String NONE_MARK = "NONE";
    String FULL_MARK = "FULL";
    String FILE_TOOL = "File";
    String SHELL_TOOL = "Shell";
    String GLOB_TOOL = "Glob";
    String GREP_TOOL = "Grep";
    String WEBFETCH_TOOL = "WebFetch";
    String WEATHER_TOOL = "Weather";
    String SKILL_TOOL = "Skill";
    List<String> FULL_LIST = List.of(FILE_TOOL, SHELL_TOOL, GLOB_TOOL, GREP_TOOL, WEBFETCH_TOOL, WEATHER_TOOL, SKILL_TOOL);

    static List<ToolInfo> getToolInfoList() {
        return List.of(new ToolInfo(FILE_TOOL, "`Read`、`Write`、`Edit` 文件的读写和修改", FileSystemTools.class),
                new ToolInfo(SHELL_TOOL, "`Bash`、`BashOutput`、`KillShell` 运行命令", ShellTools.class),
                new ToolInfo(GLOB_TOOL, "`Glob` 文件匹配", GlobTool.class),
                new ToolInfo(GREP_TOOL, "`Grep` 文本匹配", GrepTool.class),
                new ToolInfo(WEBFETCH_TOOL, "`WebFetch` 读 Web 页面内容", SmartWebFetchTool.class),
                new ToolInfo(WEATHER_TOOL, "`Weather` 获取天气", WeatherTools.class),
                new ToolInfo(SKILL_TOOL, "`Skill` 技能加载", SkillsTool.class));
    }

    @Getter
    @AllArgsConstructor
    class ToolInfo {
        private String name;
        private String description;
        private Class<?> clazz;
    }
}
