package z.caAuth.banking;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xaocut_anarchist.org.calistobanking.api.BankingAPI;
import z.caAuth.banking.BankService.Overview;
import z.caAuth.banking.BankService.Quote;
import z.caAuth.banking.BankService.Status;

import java.util.UUID;

final class BankHook {

    private BankHook() {
    }

    private static BankingAPI api(JavaPlugin plugin) {
        RegisteredServiceProvider<BankingAPI> provider =
                plugin.getServer().getServicesManager().getRegistration(BankingAPI.class);
        return provider == null ? null : provider.getProvider();
    }

    static Overview overview(JavaPlugin plugin, UUID owner) {
        BankingAPI api = api(plugin);
        if (api == null) {
            return new Overview(Status.UNAVAILABLE, null, 0);
        }
        if (!api.hasCard(owner)) {
            return new Overview(Status.NO_CARD, null, 0);
        }
        return new Overview(Status.OK, api.getCardId(owner), api.getBalance(owner));
    }

    static Quote quote(JavaPlugin plugin, UUID owner, String recipient, int amount) {
        BankingAPI api = api(plugin);
        if (api == null) {
            return failed(Status.UNAVAILABLE);
        }
        if (amount <= 0) {
            return failed(Status.INVALID_AMOUNT);
        }
        if (!api.hasCard(owner)) {
            return failed(Status.NO_CARD);
        }
        BankingAPI.CardInfo target = api.findCard(recipient);
        if (target == null) {
            return failed(Status.RECIPIENT_NOT_FOUND);
        }
        double balance = api.getBalance(owner);
        if (target.ownerUuid().equals(owner)) {
            return new Quote(Status.OWN_CARD, target.cardId(), target.ownerName(), amount, 0, 0, amount, balance);
        }
        double percent = api.getTransferFeePercent(owner);
        double fee = amount * (percent / 100.0);
        double total = amount + fee;
        Status status = balance + 1e-9 < total ? Status.INSUFFICIENT : Status.OK;
        return new Quote(status, target.cardId(), target.ownerName(), amount, percent, fee, total, balance);
    }

    static Quote transfer(JavaPlugin plugin, UUID owner, String cardId, int amount, String source) {
        Quote quote = quote(plugin, owner, cardId, amount);
        if (quote.status() != Status.OK) {
            return quote;
        }
        BankingAPI api = api(plugin);
        BankingAPI.CardInfo target = api.findCard(cardId);
        if (target == null) {
            return failed(Status.RECIPIENT_NOT_FOUND);
        }
        boolean done = api.transfer(owner, target.ownerUuid(), amount, source);
        return new Quote(done ? Status.OK : Status.ERROR, quote.cardId(), quote.recipientName(), amount,
                quote.feePercent(), quote.fee(), quote.total(), api.getBalance(owner));
    }

    private static Quote failed(Status status) {
        return new Quote(status, null, null, 0, 0, 0, 0, 0);
    }
}
