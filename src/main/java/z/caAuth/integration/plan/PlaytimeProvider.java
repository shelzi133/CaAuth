package z.caAuth.integration.plan;

import com.djrapitops.plan.query.QueryService;
import z.caAuth.config.RolesConfig;

import java.util.UUID;

public class PlaytimeProvider {

    private final QueryService queryService;
    private final RolesConfig rolesConfig;

    public PlaytimeProvider(QueryService queryService, RolesConfig rolesConfig) {
        this.rolesConfig = rolesConfig;
        this.queryService = queryService;
    }

    public long getPlaytimeLastTime(UUID playerUUID) {
        long now = System.currentTimeMillis();
        long ago = now - rolesConfig.getIntervalTime();
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(IllegalStateException::new);
        return queryService.getCommonQueries().fetchPlaytime(
                playerUUID, serverUUID, ago, now
        );
    }
}