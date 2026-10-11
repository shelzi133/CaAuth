package z.caAuth.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record SkinProfile(int slot, SkinSource source, String input, String skinId, String skinType, String variant,
                          String textureValue, String textureSignature) {

    public String textureUrl() {
        try {
            JsonObject json = JsonParser.parseString(new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
