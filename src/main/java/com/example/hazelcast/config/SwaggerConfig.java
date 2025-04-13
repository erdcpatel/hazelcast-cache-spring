package com.example.hazelcast.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hazelcast Cache Spring API")
                        .version("1.0")
                        .description("Comprehensive API documentation for the Hazelcast Cache Spring application, showcasing Hazelcast features."))
                .addTagsItem(new Tag().name("Cache API").description("Operations for managing cache entries"))
                .addServersItem(new Server().url("http://localhost:8080").description("Local server"))
                .paths(new Paths()
                        .addPathItem("/cache/{key}", new io.swagger.v3.oas.models.PathItem()
                                .put(new Operation()
                                        .summary("Add entry to cache")
                                        .description("Adds a new entry to the Hazelcast cache.")
                                        .addParametersItem(new Parameter().name("key").description("Cache key").required(true))
                                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Entry added successfully"))))
                                .get(new Operation()
                                        .summary("Retrieve entry from cache")
                                        .description("Fetches an entry from the Hazelcast cache by key.")
                                        .addParametersItem(new Parameter().name("key").description("Cache key").required(true))
                                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Entry retrieved successfully"))))
                                .delete(new Operation()
                                        .summary("Remove entry from cache")
                                        .description("Deletes an entry from the Hazelcast cache by key.")
                                        .addParametersItem(new Parameter().name("key").description("Cache key").required(true))
                                        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Entry removed successfully"))))));
    }
}