package flavor.pie.react;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;

public class ReactData extends AbstractData<ReactData, ReactData.Immutable> {

    private long gamesWon;

    {
        registerGettersAndSetters();
    }

    ReactData() {
        gamesWon = 0L;
    }

    ReactData(long gamesWon) {
        this.gamesWon = gamesWon;
    }

    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(ReactKeys.GAMES_WON, this::getGamesWon);
        registerFieldSetter(ReactKeys.GAMES_WON, this::setGamesWon);
        registerKeyValue(ReactKeys.GAMES_WON, this::gamesWon);
    }

    public long getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(long gamesWon) {
        this.gamesWon = gamesWon;
    }

    public Value<Long> gamesWon() {
        return Sponge.getRegistry().getValueFactory().createValue(ReactKeys.GAMES_WON, gamesWon);
    }

    @Override
    public Optional<ReactData> fill(DataHolder dataHolder, MergeFunction overlap) {
        dataHolder.get(ReactData.class).ifPresent(that -> {
            ReactData data = overlap.merge(this, that);
            this.gamesWon = data.gamesWon;
        });
        return Optional.of(this);
    }

    @Override
    public Optional<ReactData> from(DataContainer container) {
        return from((DataView) container);
    }

    public Optional<ReactData> from(DataView container) {
        container.getLong(ReactKeys.GAMES_WON.getQuery()).ifPresent(v -> gamesWon = v);
        return Optional.of(this);
    }

    @Override
    public ReactData copy() {
        return new ReactData(gamesWon);
    }

    @Override
    public Immutable asImmutable() {
        return new Immutable(gamesWon);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(ReactKeys.GAMES_WON.getQuery(), gamesWon);
    }

    public static class Immutable extends AbstractImmutableData<Immutable, ReactData> {

        private long gamesWon;
        {
            registerGetters();
        }

        Immutable() {
            gamesWon = 0L;
        }

        Immutable(long gamesWon) {
            this.gamesWon = gamesWon;
        }

        @Override
        protected void registerGetters() {
            registerFieldGetter(ReactKeys.GAMES_WON, this::getGamesWon);
            registerKeyValue(ReactKeys.GAMES_WON, this::gamesWon);
        }

        public long getGamesWon() {
            return gamesWon;
        }

        public ImmutableValue<Long> gamesWon() {
            return Sponge.getRegistry().getValueFactory().createValue(ReactKeys.GAMES_WON, gamesWon).asImmutable();
        }

        @Override
        public ReactData asMutable() {
            return new ReactData(gamesWon);
        }

        @Override
        public int getContentVersion() {
            return 1;
        }

        @Override
        public DataContainer toContainer() {
            return super.toContainer()
                    .set(ReactKeys.GAMES_WON.getQuery(), gamesWon);
        }

    }

    public static class Builder extends AbstractDataBuilder<ReactData> implements DataManipulatorBuilder<ReactData, Immutable> {

        protected Builder() {
            super(ReactData.class, 1);
            ReactKeys.GAMES_WON.getQuery();
        }

        @Override
        public ReactData create() {
            return new ReactData();
        }

        @Override
        public Optional<ReactData> createFrom(DataHolder dataHolder) {
            return create().fill(dataHolder);
        }

        @Override
        protected Optional<ReactData> buildContent(DataView container) throws InvalidDataException {
            return create().from(container);
        }

    }
}
