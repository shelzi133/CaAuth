package z.caAuth.hook;

import com.djrapitops.plan.query.QueryService;
import z.caAuth.bots.discord.role.config.RolesConfig;

import java.util.UUID;

public class TimeCounter {

    private final QueryService queryService;
    private final RolesConfig rolesConfig;

    public TimeCounter(QueryService queryService, RolesConfig rolesConfig) {
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