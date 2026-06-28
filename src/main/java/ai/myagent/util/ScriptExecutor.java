package ai.myagent.util;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shell 脚本执行工具类。
 *
 * 提供执行外部 shell 脚本和内联 shell 命令的能力，
 * 支持超时控制、工作目录指定、环境变量注入等。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 执行脚本
 * ScriptExecutor.Result result = ScriptExecutor.run("/path/to/script.sh");
 *
 * // 执行脚本带超时和参数
 * ScriptExecutor.Result result = ScriptExecutor.run("/path/to/vec0-install.sh", 120,
 *     "--prefix", "/usr/local");
 *
 * // 执行内联命令
 * ScriptExecutor.Result result = ScriptExecutor.exec("echo hello && pwd", 30);
 *
 * // 指定工作目录和环境变量
 * ScriptExecutor.Result result = ScriptExecutor.run("/path/to/script.sh", 60,
 *     Map.of("MY_VAR", "value"), "/path/to/workdir");
 * }</pre>
 *
 * @author yulewei
 * @since 2026/6/27
 */
@Slf4j
@UtilityClass
public class ScriptExecutor {

    /** 默认超时时间（秒） */
    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    /** Shell 名称（Unix-like） */
    private static final String SHELL_BASH = "/bin/bash";

    // ========== 结果对象 ==========

    /**
     * 脚本执行结果。
     */
    @Data
    @Builder
    public static class Result {

        /** 进程退出码（0 表示成功，-1 表示超时） */
        private final int exitCode;

        /** 标准输出 */
        @Builder.Default
        private final String output = "";

        /** 标准错误输出 */
        @Builder.Default
        private final String error = "";

        /** 是否因超时而终止 */
        private final boolean timedOut;

        /** 执行耗时（毫秒） */
        private final long durationMs;

        /**
         * 执行是否成功（退出码为 0 且未超时）。
         */
        public boolean isSuccess() {
            return exitCode == 0 && !timedOut;
        }

        /**
         * 合并所有输出（stdout + stderr）。
         */
        public String getAllOutput() {
            if (error.isEmpty()) {
                return output;
            }
            if (output.isEmpty()) {
                return error;
            }
            return output + "\n" + error;
        }
    }

    // ========== 脚本执行 ==========

    /**
     * 执行指定路径的 shell 脚本（默认 300 秒超时）。
     *
     * @param scriptPath 脚本文件路径
     * @param args       脚本参数（可选）
     * @return 执行结果
     */
    public static Result run(String scriptPath, String... args) {
        return run(scriptPath, DEFAULT_TIMEOUT_SECONDS, args);
    }

    /**
     * 执行指定路径的 shell 脚本（带超时控制）。
     *
     * @param scriptPath     脚本文件路径
     * @param timeoutSeconds 超时秒数
     * @param args           脚本参数（可选）
     * @return 执行结果
     */
    public static Result run(String scriptPath, long timeoutSeconds, String... args) {
        return run(scriptPath, timeoutSeconds, null, null, args);
    }

    /**
     * 执行指定路径的 shell 脚本（完整参数）。
     *
     * @param scriptPath     脚本文件路径
     * @param timeoutSeconds 超时秒数
     * @param extraEnv       额外环境变量（覆盖系统环境变量，可为 null）
     * @param workingDir     工作目录（null 表示脚本所在目录）
     * @param args           脚本参数（可选）
     * @return 执行结果
     */
    public static Result run(String scriptPath, long timeoutSeconds,
                             @Nullable Map<String, String> extraEnv,
                             @Nullable String workingDir,
                             String... args) {
        Path scriptFile = Paths.get(scriptPath);

        // 检查脚本是否存在
        if (!Files.exists(scriptFile)) {
            log.warn("Script not found: {}", scriptPath);
            return Result.builder()
                    .exitCode(-1)
                    .output("")
                    .error("Script not found: " + scriptPath)
                    .timedOut(false)
                    .durationMs(0)
                    .build();
        }

        // 如果脚本文件不可执行，自动添加执行权限
        if (!Files.isExecutable(scriptFile)) {
            try {
                scriptFile.toFile().setExecutable(true);
                log.debug("Set executable permission on: {}", scriptPath);
            } catch (Exception e) {
                log.warn("Failed to set executable permission on {}: {}", scriptPath, e.getMessage());
            }
        }

        // 确定工作目录
        Path workDir = resolveWorkingDir(workingDir, scriptFile);

        // 构造命令：bash script.sh [args...]
        List<String> command;
        if (args != null && args.length > 0) {
            command = new ArrayList<>(args.length + 2);
            command.add(SHELL_BASH);
            command.add(scriptFile.toAbsolutePath().toString());
            command.addAll(Arrays.asList(args));
        } else {
            command = List.of(SHELL_BASH, scriptFile.toAbsolutePath().toString());
        }

        log.debug("Executing script: {} (workDir={}, timeout={}s)", command, workDir, timeoutSeconds);
        return execute(command, extraEnv, workDir, timeoutSeconds);
    }

    // ========== 内联命令执行 ==========

    /**
     * 执行内联 shell 命令（默认 300 秒超时）。
     *
     * @param command shell 命令字符串
     * @return 执行结果
     */
    public static Result exec(String command) {
        return exec(command, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行内联 shell 命令（带超时控制）。
     *
     * @param command         shell 命令字符串
     * @param timeoutSeconds  超时秒数
     * @return 执行结果
     */
    public static Result exec(String command, long timeoutSeconds) {
        return exec(command, timeoutSeconds, null, null);
    }

    /**
     * 执行内联 shell 命令（完整参数）。
     *
     * @param command         shell 命令字符串
     * @param timeoutSeconds  超时秒数
     * @param extraEnv        额外环境变量（可为 null）
     * @param workingDir      工作目录（可为 null）
     * @return 执行结果
     */
    public static Result exec(String command, long timeoutSeconds,
                              @Nullable Map<String, String> extraEnv,
                              @Nullable String workingDir) {
        List<String> cmdLine = List.of(SHELL_BASH, "-c", command);
        Path workDir = workingDir != null ? Paths.get(workingDir) : null;

        log.debug("Executing command: {} (workDir={}, timeout={}s)", command, workDir, timeoutSeconds);
        return execute(cmdLine, extraEnv, workDir, timeoutSeconds);
    }

    // ========== 核心执行逻辑 ==========

    /**
     * 核心执行方法。
     *
     * @param command         命令列表（如 ["bash", "/path/to/script.sh"]）
     * @param extraEnv        额外环境变量
     * @param workingDir      工作目录
     * @param timeoutSeconds  超时秒数
     * @return 执行结果
     */
    private static Result execute(List<String> command,
                                  @Nullable Map<String, String> extraEnv,
                                  @Nullable Path workingDir,
                                  long timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(command);

        // 设置工作目录
        if (workingDir != null && Files.isDirectory(workingDir)) {
            pb.directory(workingDir.toFile());
        }

        // 注入额外环境变量
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }

        // 分开 stdout 和 stderr
        pb.redirectErrorStream(false);

        Process process = null;
        StringBuilder outputBuf = new StringBuilder(8192);
        StringBuilder errorBuf = new StringBuilder(1024);

        try {
            process = pb.start();

            // 并发读取 stdout 和 stderr，防止缓冲区死锁
            Thread outputThread = startReader(process.getInputStream(), outputBuf);
            Thread errorThread = startReader(process.getErrorStream(), errorBuf);

            // 等待进程结束（带超时）
            boolean finished;
            try {
                finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                finished = false;
            }

            // 等待读取线程结束（最多等 3 秒）
            try {
                outputThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                errorThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long duration = System.currentTimeMillis() - startTime;

            if (!finished) {
                // 超时，强制终止进程
                process.destroyForcibly();
                log.warn("Script timed out after {}s, forcibly terminated: {}", timeoutSeconds, command);
                return Result.builder()
                        .exitCode(-1)
                        .output(outputBuf.toString().strip())
                        .error(errorBuf.toString().strip() + "\n[ERROR] Timed out after " + timeoutSeconds + "s")
                        .timedOut(true)
                        .durationMs(duration)
                        .build();
            }

            int exitCode = process.exitValue();

            if (log.isDebugEnabled()) {
                log.debug("Script completed (exitCode={}, duration={}ms, outputLength={})",
                        exitCode, duration, outputBuf.length());
            }

            return Result.builder()
                    .exitCode(exitCode)
                    .output(outputBuf.toString().strip())
                    .error(errorBuf.toString().strip())
                    .timedOut(false)
                    .durationMs(duration)
                    .build();

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Failed to execute script: {} - {}", command, e.getMessage());
            return Result.builder()
                    .exitCode(-1)
                    .output(outputBuf.toString().strip())
                    .error("IOException: " + e.getMessage())
                    .timedOut(false)
                    .durationMs(duration)
                    .build();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 启动一个守护线程从输入流读取数据。
     */
    private static Thread startReader(InputStream inputStream, StringBuilder buffer) {
        Thread thread = new Thread(() -> {
            char[] buf = new char[4096];
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                int n;
                while ((n = reader.read(buf)) != -1) {
                    buffer.append(buf, 0, n);
                }
            } catch (IOException e) {
                log.trace("Reader thread interrupted: {}", e.getMessage());
            }
        }, "script-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * 解析工作目录：优先使用指定的目录，否则使用脚本文件所在目录。
     */
    private static Path resolveWorkingDir(@Nullable String workingDir, Path scriptFile) {
        if (workingDir != null && !workingDir.isBlank()) {
            Path dir = Paths.get(workingDir);
            if (Files.isDirectory(dir)) {
                return dir.toAbsolutePath();
            }
            log.warn("Working directory does not exist, fallback to script dir: {}", workingDir);
        }
        return scriptFile.toAbsolutePath().getParent();
    }
}
