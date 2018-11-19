package flavor.pie.react;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.bstats.sponge.MetricsLite2;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.data.Has;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(id="react", name="React", description="A little game to be played in chat.", authors="pie_flavor",
        version="1.3.0-SNAPSHOT")
public class React {
    @Inject
    Game game;
    @Inject
    PluginContainer container;
    @Inject @DefaultConfig(sharedRoot = true)
    Path path;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    @Inject
    Logger logger;
    @Inject @SuppressWarnings("unused")
    MetricsLite2 metrics;
    Config config;
    boolean inGame;
    String current;
    Task task;
    Random random;
    Instant started;

    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(BigDecimal.class), new BigDecimalSerializer());
        random = new Random();
        loadConfig();
        DataRegistration.builder()
                .dataClass(ReactData.class)
                .immutableClass(ReactData.Immutable.class)
                .builder(new ReactData.Builder())
                .manipulatorId("react_data")
                .dataName("ReactData")
                .buildAndRegister(container);
    }

    @Listener
    public void init(GameInitializationEvent e) {
        CommandSpec wins = CommandSpec.builder()
                .arguments(GenericArguments.playerOrSource(Text.of("player")))
                .executor(this::wins)
                .description(Text.of("Gets a player's win count"))
                .build();
        game.getCommandManager().register(this, wins, "wins");
    }

    private void loadConfig() throws IOException, ObjectMappingException {
        if (!Files.exists(path)) {
            game.getAssetManager().getAsset(this, "default.conf").get().copyToFile(path);
        }
        ConfigurationNode root = loader.load();
        updateConfig(root);
        config = root.getValue(Config.type);
        if (task != null) {
            task.cancel();
        }
        scheduleGame();
    }

    private void updateConfig(ConfigurationNode root) throws IOException, ObjectMappingException {
        int version = root.getNode("version").getInt();
        if (version < 3) {
            HoconConfigurationLoader assetLoader = HoconConfigurationLoader.builder()
                    .setURL(game.getAssetManager().getAsset(this, "default.conf").get().getUrl()).build();
            ConfigurationNode assetRoot = assetLoader.load();
            if (version < 2) {
                root.getNode("rewards").setValue(assetRoot.getNode("rewards").getValue());
                root.getNode("version").setValue(2);
            }
            root.getNode("min-players").setValue(assetRoot.getNode("min-players").getValue());
            root.getNode("version").setValue(3);
            loader.save(root);
        }
    }

    @Listener
    public void reload(GameReloadEvent e) throws IOException, ObjectMappingException {
        loadConfig();
    }

    private void newGame() {
        if (!game.getState().equals(GameState.SERVER_STARTED) ||
                config.minPlayers > game.getServer().getOnlinePlayers().size()) {
            return;
        }
        inGame = true;
        current = config.words.get(random.nextInt(config.words.size()));
        Text fullText = config.text.toBuilder().onHover(TextActions.showText(Text.of(current.trim()))).build();
        Text toShow = config.prefix.concat(fullText).concat(config.suffix);
        game.getServer().getBroadcastChannel().send(toShow);
        started = Instant.now();
        scheduleGame();
    }

    private void scheduleGame() {
        task = Task.builder()
                .execute(this::newGame)
                .delay(getDelay(), TimeUnit.SECONDS)
                .submit(this);
    }


    private static final Pattern ASTERISK = Pattern.compile("^\\*");
    private static final Pattern WINNER = Pattern.compile("$winner", Pattern.LITERAL);

    @Listener
    public void chat(MessageChannelEvent.Chat e, @First Player p) {
        String chat = e.getRawMessage().toPlain().trim();
        if (inGame && chat.equalsIgnoreCase(current)) {
            game.getServer().getBroadcastChannel().send(
                    Text.of(p.getName()+" has won! Time: "+
                            (Instant.now().getEpochSecond() - started.getEpochSecond()) +" seconds!"));
            inGame = false;
            config.rewards.commands.forEach(s -> game.getCommandManager().process(
                            s.startsWith("*") ? game.getServer().getConsole() : p,
                            WINNER.matcher(ASTERISK.matcher(s).replaceAll(""))
                                    .replaceAll(Matcher.quoteReplacement(p.getName()))));
            game.getServiceManager().provide(EconomyService.class).ifPresent(svc -> {
                if (config.rewards.economy.amount.compareTo(BigDecimal.ZERO) > 0) {
                    svc.getOrCreateAccount(p.getUniqueId()).ifPresent(acc ->
                            acc.deposit(config.rewards.economy.getCurrency(), config.rewards.economy.amount,
                                    game.getCauseStackManager().getCurrentCause()));
                }
            });
            p.offer(ReactKeys.GAMES_WON, p.get(ReactKeys.GAMES_WON).get() + 1);
        }

    }

    private void disable() {
        game.getEventManager().unregisterPluginListeners(this);
        game.getCommandManager().getOwnedBy(this).forEach(game.getCommandManager()::removeMapping);
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join e,
                       @Getter("getTargetEntity") @Has(value = ReactData.class, inverse = true) Player p) {
        ReactData data = p.getOrCreate(ReactData.class).get();
        p.offer(data);
    }

    public CommandResult wins(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        long wins = p.get(ReactKeys.GAMES_WON).get();
        src.sendMessage(Text.of("Player ", p.getName(), " has won ", wins, " times."));
        return CommandResult.builder()
                .successCount(1)
                .queryResult(wins > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) wins)
                .build();
    }

    private int getDelay() {
        if (config.maxDelay <= config.delay) {
            return config.delay;
        } else {
            return random.nextInt(config.maxDelay - config.delay + 1) + config.delay;
        }
    }
}
