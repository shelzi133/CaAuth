package z.caAuth.bots;

import z.caAuth.account.BindingType;

public interface ChatBot {

    BindingType type();

    void start();

    void stop();

    boolean isRunning();

    boolean sendLoginConfirmation(String bindingId, String playerName, String token, int timeoutSeconds);

    String resolveDisplay(String bindingId);
}
