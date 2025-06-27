package org.pr.dfs.service;

import java.util.Map;

public interface SystemService {
    Map<String, Object> getSystemHealth() throws Exception;
    Map<String, Object> getNodeInfo() throws Exception;
    Map<String, Object> getSystemMetrics() throws Exception;
    void recoverNode(String nodeId) throws Exception;
}
