package com.swe.networking;

import java.util.List;

public record NetworkStructure(List<List<ClientNode>> clusters, List<ClientNode> servers) {
}
