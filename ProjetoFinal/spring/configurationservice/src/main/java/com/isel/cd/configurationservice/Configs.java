package com.isel.cd.configurationservice;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.sql.DataSourceDefinitions;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "config")
@Data
public class Configs {
    private Map<String, String> knownGroups;
    private Map<String, Integer> knownServers;
    private Integer grpcServerPort;
    private Integer SpreadServerPort;
    private String SpreadGroupName;
    private String hostname;
    private Boolean local;
}
