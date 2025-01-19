package org.tanzu.mcp.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class AppAdvisorFunctions {
    @Value("${ADVISOR_SERVER:http://localhost:8080}") private String appAdvisorUrl;
    private static final Logger logger = LoggerFactory.getLogger(AppAdvisorFunctions.class);

    public String advisorBuildConfigGet(String pathToSourceCode) throws IOException, InterruptedException {
        return executeCommand("advisor build-config get -p " + pathToSourceCode);
    }

    public String advisorUpgradePlanGet(String pathToSourceCode) throws IOException, InterruptedException {
        return executeCommand("advisor upgrade-plan get -p " + pathToSourceCode + " -u " + appAdvisorUrl);
    }

    public String advisorUpgradePlanApply(String pathToSourceCode) throws IOException, InterruptedException {
        return executeCommand("advisor upgrade-plan apply -p " + pathToSourceCode + " -u " + appAdvisorUrl);
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("ADVISOR_SERVER", appAdvisorUrl);
        logger.info("Executing command: " + command);
        logger.info("Advisor server url: " + appAdvisorUrl);

        // Capture the output
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for the process to complete and get exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            output.append("Process did not complete successfully. Exit code: ").append(exitCode).append("\n");
        }

        return output.toString().trim();
    }

}
