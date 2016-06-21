package flavor.pie.react;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Plugin(id="react",name="React",description="A little game to be played in chat.",authors="pie_flavor",version="1.0.0")
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
        if (root.getNode("version").isVirtual()) {
            try {
                game.getAssetManager().getAsset(this, "default.conf").get().copyToFile(path);
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
    void loadConfig() throws ObjectMappingException {
        TypeToken<Text> textToken = TypeToken.of(Text.class);
        words = root.getNode("words").getList(TypeToken.of(String.class));
        prefix = root.getNode("prefix").getValue(textToken, Text.of());
        text = root.getNode("text").getValue(textToken, Text.of());
        suffix = root.getNode("suffix").getValue(textToken, Text.of());
        delay = root.getNode("delay").getInt();
        if (task != null) task.cancel();
        task = Task.builder().execute(this::newGame).delay(delay, TimeUnit.SECONDS).interval(delay, TimeUnit.SECONDS).name("react-S-createGame").submit(this);
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
        }
    }
    void disable() {
        game.getEventManager().unregisterPluginListeners(this);
        game.getCommandManager().getOwnedBy(this).forEach(game.getCommandManager()::removeMapping);
    }
}
