package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillsVo {
    @Schema(description = "Skill所属目录")
    String dir;

    @Schema(description = "Skill列表")
    List<SkillVo> skills;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillVo {
        @Schema(description = "Skill名称")
        String name;
        @Schema(description = "Skill描述")
        String description;
    }
}
