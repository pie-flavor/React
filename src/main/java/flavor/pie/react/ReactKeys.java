package flavor.pie.react;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;

public class ReactKeys {

    private ReactKeys() {}

    public final static Key<Value<Long>> GAMES_WON;
    static {
        TypeToken<Long> longToken = TypeToken.of(Long.class);
        TypeToken<Value<Long>> valueLongToken = new TypeToken<Value<Long>>(){};
        GAMES_WON = Key.builder()
                .type(valueLongToken)
                .query(DataQuery.of("GamesWon"))
                .id("react:gameswon")
                .name("Games Won")
                .build();
    }
}
