package karm.van.service.rollBack;

import java.util.Map;

public interface RollbackHandler {
    void handle(Map<String, Object> params);
}
