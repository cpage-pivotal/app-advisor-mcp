package org.tanzu.mcp.advisor;

import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class AppAdvisorFunctions {
    @Value("${ADVISOR_SERVER:http://localhost:8080}") private String appAdvisorUrl;

    public Function<Map<String, Object>, McpSchema.CallToolResult> advisorBuildConfigGetFunction() {
        return arguments -> {
            String pathToSourceCode = (String) arguments.get("pathToSourceCode");
            return executeCommand("advisor build-config get -p " + pathToSourceCode);
        };
    }

    public Function<Map<String, Object>, McpSchema.CallToolResult> advisorUpgradePlanGetFunction() {
        return arguments -> {
            String pathToSourceCode = (String) arguments.get("pathToSourceCode");
            return executeCommand("advisor upgrade-plan get -p " + pathToSourceCode);
        };
    }

    public Function<Map<String, Object>, McpSchema.CallToolResult> advisorUpgradePlanApplyFunction() {
        return arguments -> {
            String pathToSourceCode = (String) arguments.get("pathToSourceCode");
            return executeCommand("advisor upgrade-plan apply -p " + pathToSourceCode);
        };
    }

    private McpSchema.CallToolResult executeCommand(String command) {
        boolean isError = false;
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("ADVISOR_SERVER", appAdvisorUrl);

        // Capture the output
        Process process;
        StringBuilder output = new StringBuilder();
        try {
            process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            return new McpSchema.CallToolResult(List.of(
                    new McpSchema.TextContent("Could not create process executor: " + e.getMessage())), true);
        }

        // Wait for the process to complete and get exit code
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            return new McpSchema.CallToolResult(List.of(
                    new McpSchema.TextContent(output.toString()),
                    new McpSchema.TextContent("Process was interrupted: " + e.getMessage())), true);
        }
        if (exitCode != 0) {
            isError = true;
            output.append("Process did not complete successfully. Exit code: ").append(exitCode).append("\n");
        }

        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(output.toString())), isError);
    }
}
