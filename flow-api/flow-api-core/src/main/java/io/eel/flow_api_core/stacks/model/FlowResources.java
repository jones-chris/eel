package io.eel.flow_api_core.stacks.model;

import java.util.Map;
import io.eel.common.model.Flow;

public record FlowResources(
        String canonicalId,
        String flowId,
        int version,
        Map<ResourceType, String> resources
) {

    public static FlowResources from(Flow flow, Map<ResourceType, String> resources) {
        return new FlowResources(
                flow.getCanonicalId(),
                flow.getId().toString(),
                flow.getVersion(),
                resources
        );
    }

}
