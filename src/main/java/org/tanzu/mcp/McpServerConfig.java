package org.tanzu.mcp;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.tanzu.mcp.advisor.AppAdvisorFunctions;
import org.springframework.ai.mcp.server.McpAsyncServer;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

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
			.resources(false, true) // No subscribe support, but list changes notifications
			.tools(true) // Tool support with list changes notifications
			.prompts(true) // Prompt support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		var server = McpServer.using(transport).
				serverInfo("Spring Application Advisor MCP Server", "1.0.0").
				capabilities(capabilities).
				tools(cfToolRegistrations()).
				async();
		
		return server;
	}

	public List<McpServer.ToolRegistration> cfToolRegistrations() {

		return ToolHelper.toToolRegistration(
				// Applications
				FunctionCallback.builder().
						method("advisorBuildConfigGet", String.class).
						targetObject(appAdvisorFunctions).
						description("Generate the build configuration of the source code repository given the directory " +
								"path to the source code. Returns the output of the process.").
						build(),
				FunctionCallback.builder().
						method("advisorUpgradePlanGet", String.class).
						targetObject(appAdvisorFunctions).
						description("""
                                Get the upgrade plan of the source code repository given the directory path to the source code. " +
                                    "\\"Note that it depends on advisor-build-config-get to generate the build-configuration file. " +
                                    "\\"That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist.
                                    "\\"Returns the output of the process.""").
						build(),
				FunctionCallback.builder().
						method("advisorUpgradePlanApply", String.class).
						targetObject(appAdvisorFunctions).
						description("""
                                Apply the first step of the upgrade plan of the source code repository given the directory path to the source code. " +
                                    "\\"Note that it depends on advisor-build-config-get to generate the build-configuration file. " +
                                    "\\"That command must be executed first if the file in the relative project path: target/.advisor/build-config.json does not exist.
                                    "\\\\"Returns the output of the process.""").
						build()
		);
	}
}
