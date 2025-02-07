package org.tanzu.mcp.advisor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class AppAdvisorFunctions {
    @Value("${ADVISOR_SERVER:http://localhost:8080}")
    private String appAdvisorUrl;

    private static final String ADVISOR_BUILD_CONFIG_GET = "Generate the build configuration of the source " +
            "code repository. This configuration can be used to perform version upgrades of Spring applications." +
            "Returns the output of the process.";
    private static final String ADVISOR_BUILD_CONFIG_GET_PARAM = "The full directory path to the source code. Use the " +
            "path of the current IDE project if possible. If you can not determine the path yourself, ask the user to " +
            "provide it.";

    @Tool(description = ADVISOR_BUILD_CONFIG_GET)
    public String advisorBuildConfigGetFunction(@ToolParam(description = ADVISOR_BUILD_CONFIG_GET_PARAM) String pathToSourceCode) {
        return executeCommand("advisor build-config get -p " + pathToSourceCode);
    }

    private static final String ADVISOR_UPGRADE_PLAN_GET = "Get the upgrade plan of the source code repository. " +
            "This function depends on advisorBuildConfigGet to generate the build configuration file. " +
            "That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist. " +
            "Returns the output of the process.";
    private static final String ADVISOR_UPGRADE_PLAN_GET_PARAM = "The full directory path to the source code. Use the " +
            "path of the current IDE project if possible. If you can not determine the path yourself, ask the user to " +
            "provide it.";

    @Tool(description = ADVISOR_UPGRADE_PLAN_GET)
    public String advisorUpgradePlanGetFunction(@ToolParam(description = ADVISOR_UPGRADE_PLAN_GET_PARAM) String pathToSourceCode) {
        return executeCommand("advisor upgrade-plan get -p " + pathToSourceCode);
    }

    private static final String ADVISOR_UPGRADE_PLAN_APPLY = "Apply the first step of the upgrade plan of the source code repository. " +
            "This function depends on advisorBuildConfigGet to generate the build-configuration file. " +
            "That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist. " +
            "Verify with the user before performing the apply. " +
            "Use this method to perform all version upgrades of Spring applications. Returns the output of the process.";
    private static final String ADVISOR_UPGRADE_PLAN_APPLY_PARAM = "The full directory path to the source code. Use the " +
            "path of the current IDE project if possible. If you can not determine the path yourself, ask the user to " +
            "provide it.";

    @Tool(description = ADVISOR_UPGRADE_PLAN_APPLY)
    public String advisorUpgradePlanApplyFunction(@ToolParam(description = ADVISOR_UPGRADE_PLAN_APPLY_PARAM) String pathToSourceCode) {
        return executeCommand("advisor upgrade-plan apply -p " + pathToSourceCode);
    }

    private String executeCommand(String command) {
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
            throw new RuntimeException("Could not create process executor: " + e.getMessage());
        }

        // Wait for the process to complete and get exit code
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Process was interrupted: " + e.getMessage());
        }
        if (exitCode != 0) {
            throw new RuntimeException("Process did not complete successfully. Exit code: " + exitCode);
        }

        return output.toString();
    }
}
