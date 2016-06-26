package flavor.pie.react;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Plugin(id="react",name="React",description="A little game to be played in chat.",authors="pie_flavor",version="1.2.4")
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
    ConfigurationNode root;
    boolean inGame;
    List<String> words;
    int delay;
    Text prefix;
    Text text;
    Text suffix;
    String current;
    Task task;
    Random random;
    Instant started;
    List<String> commands;
    double reward;
    Currency currency;
    int minPlayers;
    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        random = new Random();
        try {
            root = loader.load();
        } catch (IOException ex) {
            logger.error("Could not load config!");
            disable();
            throw ex;
        }
        if (root.getNode("version").getInt() < 1) {
            try {
                game.getAssetManager().getAsset(this, "default.conf").get().copyToFile(path);
                root = loader.load();
            } catch (IOException ex) {
                logger.error("Could not copy default config!");
                disable();
                throw ex;
            }
        }
        try {
            loadConfig();
        } catch (ObjectMappingException ex) {
            logger.error("Could not parse config!");
            disable();
            throw ex;
        }
    }
    void loadConfig() throws IOException, ObjectMappingException {
        updateConfig();
        TypeToken<Text> textToken = TypeToken.of(Text.class);
        TypeToken<String> stringToken = TypeToken.of(String.class);
        words = root.getNode("words").getList(stringToken);
        prefix = root.getNode("prefix").getValue(textToken, Text.of());
        text = root.getNode("text").getValue(textToken, Text.of());
        suffix = root.getNode("suffix").getValue(textToken, Text.of());
        delay = root.getNode("delay").getInt();
        commands = root.getNode("rewards", "commands").getList(stringToken, Lists.newArrayList());
        minPlayers = root.getNode("min-players").getInt();
        game.getServiceManager().provide(EconomyService.class).ifPresent(svc -> {
            reward = root.getNode("rewards", "economy", "amount").getDouble(0);
            try {
                currency = root.getNode("rewards", "economy", "currency").getValue(TypeToken.of(Currency.class), svc.getDefaultCurrency());
            } catch (ObjectMappingException ex) {
                currency = svc.getDefaultCurrency();
            }
        });
        if (task != null) task.cancel();
        task = Task.builder().execute(this::newGame).delay(delay, TimeUnit.SECONDS).interval(delay, TimeUnit.SECONDS).name("react-S-createGame").submit(this);
    }
    void updateConfig() throws IOException, ObjectMappingException {
        int version = root.getNode("version").getInt();
        HoconConfigurationLoader assetLoader = HoconConfigurationLoader.builder().setURL(game.getAssetManager().getAsset(this, "default.conf").get().getUrl()).build();
        ConfigurationNode assetRoot = assetLoader.load();
        if (version < 2) {
            root.getNode("rewards").setValue(assetRoot.getNode("rewards").getValue());
            root.getNode("version").setValue(2);
        }
        if (version < 3) {
            root.getNode("min-players").setValue(assetRoot.getNode("min-players").getValue());
            root.getNode("version").setValue(3);
        }
        loader.save(root);
    }
    @Listener
    public void reload(GameReloadEvent e) throws IOException, ObjectMappingException {
        try {
            root = loader.load();
        } catch (IOException ex) {
            logger.error("Could not load config!");
            throw ex;
        }
        try {
            loadConfig();
        } catch (ObjectMappingException ex) {
            logger.error("Could not parse config!");
            throw ex;
        }
    }
    void newGame() {
        if (!game.getState().equals(GameState.SERVER_STARTED)) return;
        if (minPlayers > game.getServer().getOnlinePlayers().size()) return;
        inGame = true;
        current = words.get(random.nextInt(words.size()));
        Text fullText = text.toBuilder().onHover(TextActions.showText(Text.of(current.trim()))).build();
        Text toShow = prefix.concat(fullText).concat(suffix);
        game.getServer().getBroadcastChannel().send(toShow);
        started = Instant.now();
    }
    @Listener
    public void chat(MessageChannelEvent.Chat e, @First Player p) {
        String chat = e.getRawMessage().toPlain().trim();
        if (inGame && chat.equalsIgnoreCase(current)) {
            game.getServer().getBroadcastChannel().send(Text.of(p.getName()+" has won! Time: "+(Instant.now().getEpochSecond() - started.getEpochSecond()) +" seconds!"));
            inGame = false;
            commands.forEach(s -> game.getCommandManager().process(s.startsWith("*") ? game.getServer().getConsole() : p, s.replaceAll("^\\*", "").replace("$winner", p.getName())));
            game.getServiceManager().provide(EconomyService.class).ifPresent(svc -> {
                if (reward > 0) {
                    svc.getOrCreateAccount(p.getUniqueId()).ifPresent(acc -> acc.deposit(currency, BigDecimal.valueOf(reward), Cause.builder().from(e.getCause()).named("PluginReact", container).build()));
                }
            });
        }

    }
    void disable() {
        game.getEventManager().unregisterPluginListeners(this);
        game.getCommandManager().getOwnedBy(this).forEach(game.getCommandManager()::removeMapping);
    }
}
