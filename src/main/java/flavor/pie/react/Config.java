package flavor.pie.react;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class Config {
    public final static TypeToken<Config> type = TypeToken.of(Config.class);

    @Setting public Text text = Text.of(TextColors.YELLOW, "Hover over this text to see a word. The first person to type it wins!");
    @Setting public Text prefix = Text.EMPTY;
    @Setting public Text suffix = Text.EMPTY;
    @Setting public List<String> words = Collections.emptyList();
    @Setting public int delay = 300;
    @Setting("min-players") public int minPlayers = 5;
    @Setting public RewardsBlock rewards = new RewardsBlock();

    @ConfigSerializable
    public static class RewardsBlock {
        @Setting public EconomySection economy = new EconomySection();
        @Setting public List<String> commands = Collections.emptyList();
    }

    @ConfigSerializable
    public static class EconomySection {
        @Setting("currency") private String currencyString;
        private Currency currency;
        public Currency getCurrency() {
            if (currency == null) {
                currency = Sponge.getRegistry().getType(Currency.class, currencyString).get();
            }
            return currency;
        }
        @Setting public BigDecimal amount = BigDecimal.ZERO;
    }
}
