package org.tanzu.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.McpAsyncServer;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.tanzu.mcp.advisor.AppAdvisorFunctions;

@Configuration
@EnableWebMvc
public class McpServerConfig implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);
	private final AppAdvisorFunctions appAdvisorFunctions;

	public McpServerConfig(AppAdvisorFunctions cfFunctions) {
		this.appAdvisorFunctions = cfFunctions;
	}

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebMvcSseServerTransport webMvcSseServerTransport() {
		return new WebMvcSseServerTransport(new ObjectMapper(), "/mcp/message");
	}

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransport transport) {
		return transport.getRouterFunction();
	}

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransport stdioServerTransport() {
		return new StdioServerTransport();
	}

	@Bean
	public McpAsyncServer mcpServer(ServerMcpTransport transport) {

		var capabilities = McpSchema.ServerCapabilities.builder()
			.resources(false, false)
			.tools(true) // Tool support with list changes notifications
			.prompts(false)
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		var server = McpServer.async(transport).
				serverInfo("Spring Application Advisor MCP Server", "1.0.0").
				capabilities(capabilities).
				tools(getBuildConfigTool(),getUpgradePlanGetTool(),getUpgradePlanApplyTool()).
				build();
		
		return server;
	}

	private static final String DESCRIPTION_ADVISOR_BUILD_CONFIG_GET = "Generate the build configuration of the source " +
			"code repository. This configuration can be used to perform version upgrades of Spring applications.Returns the output of the process.";
	private McpServerFeatures.AsyncToolRegistration getBuildConfigTool() {
		return new McpServerFeatures.AsyncToolRegistration(
				new McpSchema.Tool("advisorBuildConfigGet", DESCRIPTION_ADVISOR_BUILD_CONFIG_GET,
				"""
						{
							"type": "object",
							"properties": {
								"pathToSourceCode": {
									"type": "string",
									"description": "The directory path to the source code. Use the path of the current IDE project if possible. If you can not determine the path yourself, ask the user to provide it."
								}
							},
							"required": ["pathToSourceCode"]
						}
						"""),
				appAdvisorFunctions.advisorBuildConfigGetFunction());
	}

	private static final String DESCRIPTION_ADVISOR_UPGRADE_PLAN_GET = "Get the upgrade plan of the source code repository. " +
			"This function depends on advisorBuildConfigGet to generate the build configuration file. " +
			"That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist. " +
			"Returns the output of the process.";
	private McpServerFeatures.AsyncToolRegistration getUpgradePlanGetTool() {
		return new McpServerFeatures.AsyncToolRegistration(
				new McpSchema.Tool("advisorUpgradePlanGet", DESCRIPTION_ADVISOR_UPGRADE_PLAN_GET,
						"""
                                {
                                    "type": "object",
                                    "properties": {
                                        "pathToSourceCode": {
                                            "type": "string",
                                            "description": "The directory path to the source code. Use the path of the current IDE project if possible. If you can not determine the path yourself, ask the user to provide it."
                                        }
                                    },
                                    "required": ["pathToSourceCode"]
                                }
                                """),
				appAdvisorFunctions.advisorUpgradePlanGetFunction());
	}

	private static final String DESCRIPTION_ADVISOR_UPGRADE_PLAN_APPLY = "Apply the first step of the upgrade plan of the source code repository. " +
			"This function depends on advisorBuildConfigGet to generate the build-configuration file. " +
			"That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist. " +
			"Verify with the user before performing the apply. " +
			"Use this method to perform all version upgrades of Spring applications. Returns the output of the process.";
	private McpServerFeatures.AsyncToolRegistration getUpgradePlanApplyTool() {
		return new McpServerFeatures.AsyncToolRegistration(
				new McpSchema.Tool("advisorUpgradePlanApply", DESCRIPTION_ADVISOR_UPGRADE_PLAN_APPLY,
						"""
                                {
                                    "type": "object",
                                    "properties": {
                                        "pathToSourceCode": {
                                            "type": "string",
                                            "description": "The directory path to the source code. Use the path of the current IDE project if possible. If you can not determine the path yourself, ask the user to provide it."
                                        }
                                    },
                                    "required": ["pathToSourceCode"]
                                }
                                """),
				appAdvisorFunctions.advisorUpgradePlanApplyFunction());
	}
}
