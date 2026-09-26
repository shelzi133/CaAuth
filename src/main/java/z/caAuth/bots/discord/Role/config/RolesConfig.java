package z.caAuth.bots.discord.role.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.NameModifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class RolesConfig extends OkaeriConfig {


    @Comment("Вкл/выкл автовыдачу ролей на дс сервере")
    private boolean enabled = false;
    private String roleId = "";
    @Comment(" ")

    @Comment({" ", "Автосмена ника на дс сервере"})
    private boolean changeNickname = false;

    public ActivePlayer activePlayer = new ActivePlayer();

    public static class ActivePlayer extends OkaeriConfig {

        @Comment("Вкл/выкл автовыдачу активной роли")
        private boolean activePlayerRole = false;
        private String activeRoleId = "";

        @Comment("Через сколько выдаются роли (в тиках)")
        private int period = 18000;

        public IntervalTime intervalTime = new IntervalTime();
        public PlayerTime playerTime = new PlayerTime();

    }
    public static class PlayerTime extends OkaeriConfig {
        @Comment("Время отыгрыша для выдачи роли")
        private int second = 0;
        private int minute = 0;
        private int hours = 0;
        private int day = 0;
    }

    public static class IntervalTime extends OkaeriConfig {
        private int second = 0;
        private int minute = 0;
        private int hours = 0;
        private int day = 0;
    }


    @Comment("Вкл/выкл автовыдачу донат ролей (Нужен LuckPerms)")
    private boolean donateRole = false;
    private Map<String, String> donateGroups = new LinkedHashMap<>() {{
        put("imba", "");
        put("slay", "");
        put("prime", "");
    }};

    @Comment(" ")
    @Comment("Вкл/выкл автовыдачу ролей персонала (Нужен LuckPerms)")
    private boolean staffRole = false;
    @Comment(" ")
    @Comment("id общей роли персонала")
    private String staffId = "";
    private Map<String, String> staffGroups = new LinkedHashMap<>() {{
        put("helper", "");
        put("moder", "");
        put("padmin", "");
    }};


    public void reload() {
        this.load(true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean getActivePlayerRole(){
        return activePlayer.activePlayerRole;
    }

    public long getRequiredTime(){
        long requiredTime = activePlayer.playerTime.second * 1000L + activePlayer.playerTime.minute * 60000L + activePlayer.playerTime.hours * 3600000L + activePlayer.playerTime.day * 86400000L;
        return requiredTime;
    }

    public long getIntervalTime(){
        long intervalTime = activePlayer.intervalTime.second * 1000L + activePlayer.intervalTime.minute * 60000L + activePlayer.intervalTime.hours * 3600000L + activePlayer.intervalTime.day * 86400000L;
        return intervalTime;
    }

    public int getPeriod(){
        return activePlayer.period;
    }

    public String getActiveRoleId() {
        return activePlayer.activeRoleId;
    }

    public String getRoleId() {
        return roleId;
    }

    public boolean isDonateRole() {
        return donateRole;
    }

    public Set<String> getDonateGroups() {
        return groupNames(donateGroups);
    }

    public String getRoleIdForGroup(String group) {
        return roleIdFor(donateGroups, group);
    }

    public boolean isStaffRole() {
        return staffRole;
    }

    public String getStaffId() {
        return staffId;
    }

    public Set<String> getStaffGroups() {
        return groupNames(staffGroups);
    }

    public String getStaffRoleIdForGroup(String group) {
        return roleIdFor(staffGroups, group);
    }

    public boolean isChangeNicknameEnabled() {
        return changeNickname;
    }

    private Set<String> groupNames(Map<String, String> groups) {
        Set<String> result = new LinkedHashSet<>();
        for (String key : groups.keySet()) {
            result.add(key.toLowerCase());
        }
        return result;
    }

    private String roleIdFor(Map<String, String> groups, String group) {
        if (group == null || group.isBlank()) return "";
        return groups.getOrDefault(group.toLowerCase(), "");
    }
}