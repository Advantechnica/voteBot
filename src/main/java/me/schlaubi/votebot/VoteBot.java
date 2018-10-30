package me.schlaubi.votebot;


import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import me.schlaubi.votebot.commands.general.HelpCommand;
import me.schlaubi.votebot.commands.settings.LanguageCommand;
import me.schlaubi.votebot.commands.settings.PrefixCommand;
import me.schlaubi.votebot.commands.vote.CloseCommand;
import me.schlaubi.votebot.commands.vote.CreateCommand;
import me.schlaubi.votebot.commands.vote.InfoCommand;
import me.schlaubi.votebot.core.GameAnimator;
import me.schlaubi.votebot.core.VoteExecutor;
import me.schlaubi.votebot.core.VoteManager;
import me.schlaubi.votebot.core.cache.Cache;
import me.schlaubi.votebot.core.command.CommandManager;
import me.schlaubi.votebot.core.entities.Guild;
import me.schlaubi.votebot.core.entities.User;
import me.schlaubi.votebot.core.events.AllShardsLoadedEvent;
import me.schlaubi.votebot.core.translation.TranslationManager;
import me.schlaubi.votebot.io.config.Configuration;
import me.schlaubi.votebot.io.database.Cassandra;
import me.schlaubi.votebot.listener.CommandLogger;
import me.schlaubi.votebot.listener.SelfMentionListener;
import me.schlaubi.votebot.listener.ShardsListener;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.hooks.AnnotatedEventManager;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.SubscribeEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.Closeable;
import java.io.IOException;

@Log4j2
public class VoteBot implements Closeable {

    @Getter
    private static VoteBot instance;
    @Getter
    private final Cassandra databaseConnection;
    @Getter
    private final Configuration configuration;
    @Getter
    private final OkHttpClient httpClient;
    @Getter
    private final IEventManager eventManager;
    @Getter
    private ShardManager shardManager;
    @Getter
    private boolean allShardsInitialized = false;
    @Getter
    private final Cache<Guild> guildCache;
    @Getter
    private final Cache<User> userCache;
    @Getter
    private final CommandManager commandManager;
    @Getter
    private final boolean debug;
    @Getter
    private final TranslationManager translationManager;
    @Getter
    private final VoteManager voteManager;

    public VoteBot(Cassandra databaseConnection, Configuration configuration, boolean debug) {
        instance = this;
        this.debug = debug;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        this.databaseConnection = databaseConnection;
        this.configuration = configuration;
        httpClient = new OkHttpClient();
        eventManager = new AnnotatedEventManager();
        guildCache = new Cache<>(Guild.class, Guild.GuildProvider.class, databaseConnection);
        userCache = new Cache<>(User.class, User.UserProvider.class, databaseConnection);
        translationManager = new TranslationManager();
        voteManager = new VoteManager(this);
        commandManager = new CommandManager(debug ? configuration.getString("prefixes.debug") : configuration.getString("prefixes.default"), this);
        initializeShardManager();
        registerCommands();
    }

    private void initializeShardManager() {
        DefaultShardManagerBuilder shardManagerBuilder = new DefaultShardManagerBuilder()
                .setHttpClient(httpClient)
                .setEventManagerProvider((id) -> eventManager)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.playing("Starting ..."))
                .setToken(configuration.getString("bot.token"))
                .setShardsTotal(retrieveShards())
                .addEventListeners(
                        new ShardsListener(),
                        new CommandLogger(),
                        new SelfMentionListener(),
                        new VoteExecutor(voteManager.getCache()),
                        this,
                        commandManager
                );
        try {
            this.shardManager = shardManagerBuilder.build();
        } catch (LoginException e) {
            log.fatal("[ShardManager] Error while connecting to Discord", e);
            close();
        }
    }

    private int retrieveShards() {
        Request request = new Request.Builder()
                .url("https://discordapp.com/api/gateway/bot")
                .addHeader("Authorization", configuration.getString("bot.token"))
                .get()
                .build();
        try (var response = httpClient.newCall(request).execute()) {
            assert response.body() != null;
            int shardCount = new JSONObject(response.body().string()).getInt("shards");
            log.info(String.format("[ShardManager] Starting with %d shards", shardCount));
            return shardCount;
        } catch (IOException | JSONException e) {
            log.error("[ShardManager] Couldn't retrieve shard-count! Using default value instead!", e);
            return configuration.getInt("bot.defaultshards");
        }
    }

    public void registerCommands() {
        commandManager.registerCommands(
                new HelpCommand(),
                new PrefixCommand(),
                new LanguageCommand(),
                new CreateCommand(),
                new CloseCommand(),
                new InfoCommand()
        );
    }

    @Override
    public void close() {
        if (databaseConnection != null)
            databaseConnection.close();
        if (shardManager != null)
            shardManager.shutdown();
        System.exit(1);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    private void onReady(AllShardsLoadedEvent event) {
        allShardsInitialized = true;
        new GameAnimator(shardManager, configuration);
    }
}
