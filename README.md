# Spring Application Advisor MCP Server

This MCP Server provides an LLM interface for interacting with [Spring Application Advisor](https://techdocs.broadcom.com/us/en/vmware-tanzu/spring/tanzu-spring/commercial/spring-tanzu/app-advisor-what-is-app-advisor.html). It was built with the [Spring AI MCP](https://spring.io/blog/2024/12/11/spring-ai-mcp-announcement) project, based on an implementation by [@Albertoimpl](https://github.com/Albertoimpl)

![Sample](images/sample.png)

## Prerequisites

You need to install the [Advisor CLI](https://techdocs.broadcom.com/us/en/vmware-tanzu/spring/tanzu-spring/commercial/spring-tanzu/app-advisor-run-app-advisor-cli.html) in the path on your machine.

## Building the Server

```bash
./mvnw clean package
```

## Configuration

You will need to supply a configuration for the server for your MCP Client. Here's what the configuration looks like for [claude_desktop_config.json](https://modelcontextprotocol.io/quickstart/user):

```
{
  "mcpServers": {
    "app-advisor": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.transport=stdio", "-Dlogging.file.name=app-advisor-mcp.log", "-jar" ,
        "/Users/pcorby/Projects/OpenAI/app-advisor-mcp/target/app-advisor-mcp-0.0.1-SNAPSHOT.jar",
        "--server.port=8041"
      ],
      "env": {
        "ADVISOR_SERVER": "http://localhost:8080"
      }
  }
}
```
