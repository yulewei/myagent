package ai.myagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {

    /**
     * 文件key
     */
    private String fileKey;

    /**
     * 原始文件名
     */
    private String originalFilename;
}
