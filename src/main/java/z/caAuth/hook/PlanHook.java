package z.caAuth.hook;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.query.QueryService;
import z.caAuth.bots.discord.role.config.RolesConfig;

import java.util.Optional;

public class PlanHook {

    private final RolesConfig rolesConfig;

    public PlanHook(RolesConfig rolesConfig) {
        this.rolesConfig = rolesConfig;
    }

    public Optional<TimeCounter> hookIntoPlan() {
        CapabilityService capabilities = CapabilityService.getInstance();
        if (!capabilities.hasCapability("QUERY_API")) return Optional.empty();
        try {
            return Optional.of(new TimeCounter(QueryService.getInstance(), rolesConfig));
        } catch (IllegalStateException planNotEnabled) {
            return Optional.empty();
        }
    }
}
