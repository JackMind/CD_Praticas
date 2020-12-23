package com.isel.cd.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Server {

    private String name;
    private String ip;
    private int port;
}
