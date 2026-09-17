package z.caAuth.util;


public enum BindingType {
    DISCORD("discord", "Discord"),
    TELEGRAM("telegram", "Telegram");
    
    private final String id;
    private final String displayName;
    
    BindingType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static BindingType fromString(String type) {
        if (type == null) {
            return null;
        }
        
        for (BindingType bindingType : values()) {
            if (bindingType.id.equalsIgnoreCase(type)) {
                return bindingType;
            }
        }
        
        return null;
    }
}

