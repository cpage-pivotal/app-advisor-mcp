package org.tanzu.mcp;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.mcp.advisor.AppAdvisorFunctions;

import java.util.List;

@Configuration
public class McpServerConfig {

	@Bean
	public List<ToolCallback> roleplayTools(AppAdvisorFunctions appAdvisorFunctions) {
		return List.of(ToolCallbacks.from(appAdvisorFunctions));
	}
}
