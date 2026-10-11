package z.caAuth.skins;

import java.util.UUID;

@FunctionalInterface
public interface SkinsBotBridge {
    boolean requestPng(UUID account, int slot);
}
