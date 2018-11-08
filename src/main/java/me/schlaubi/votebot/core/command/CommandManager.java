package me.schlaubi.votebot.core.command;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import me.schlaubi.votebot.VoteBot;
import me.schlaubi.votebot.core.events.CommandExecutedEvent;
import me.schlaubi.votebot.core.events.CommandFailedEvent;
import me.schlaubi.votebot.core.events.CommandPermissionViolationEvent;
import me.schlaubi.votebot.util.NameThreadFactory;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class CommandManager implements Closeable {

    @Getter
    private final Map<String, Command> commandAssociations;
    private final String defaultPrefix;
    private final VoteBot bot;
    private final ExecutorService executor;

    public CommandManager(@NotNull String defaultPrefix, VoteBot bot) {
        commandAssociations = new HashMap<>();
        this.defaultPrefix = defaultPrefix;
        this.bot = bot;
        this.executor = Executors.newCachedThreadPool(new NameThreadFactory("CommandExecutor"));
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    private void onMessageReceived(GuildMessageReceivedEvent event) {
        //Check if all shards are running
        if (!bot.isAllShardsInitialized()) {
            return;
        }
        //Check if event is not a CommandEvent
        if (event instanceof CommandEvent) {
            return;
        }
        //Check if invoker is real
        User author = event.getAuthor();
        if (author.isBot() || author.isFake() || event.isWebhookMessage())
            return;
        executor.execute(() -> parseCommands(event));
    }

    private void parseCommands(GuildMessageReceivedEvent event) {
        String prefix = null;
        String content = event.getMessage().getContentRaw();
        //Check prefix
        if (content.startsWith(defaultPrefix))
            prefix = defaultPrefix;
        else {
            String mention = event.getGuild().getSelfMember().getAsMention();
            if (content.startsWith(mention))
                prefix = mention;
            else {
                String customPrefix = bot.getGuildCache().get(event.getGuild()).getPrefix();
                if (content.startsWith(customPrefix))
                    prefix = customPrefix;
            }
        }
        //Abort if message don't start with the right prefix
        if (prefix == null)
            return;
        //Remove prefix
        String beheaded = content.substring(prefix.length()).trim();
        //Split arguments
        String[] allArgs = beheaded.split("\\s+");
        String invocation = allArgs[0].toLowerCase();
        //Search for commands
        if (!commandAssociations.containsKey(invocation))
            return;
        Command command = commandAssociations.get(invocation);
        //Remove invocation
        String[] commandArgs = new String[allArgs.length - 1];
        System.arraycopy(allArgs, 1, commandArgs, 0, commandArgs.length);
        String[] args;
        //Check for sub commands
        if (commandArgs.length > 0 && command.getSubCommandAssociations().containsKey(commandArgs[0])) {
            command = command.getSubCommandAssociations().get(commandArgs[0]);
            args = new String[commandArgs.length -1];
            System.arraycopy(commandArgs, 1, args, 0, args.length);
        } else
            args = commandArgs;
        CommandEvent commandEvent = new CommandEvent(event, command, bot, args, invocation);
        callCommand(command, commandEvent);
    }

    private void callCommand(Command command, CommandEvent commandEvent) {
        //Check permission
        if (!command.getPermissions().isCovered(bot.getUserCache().get(commandEvent.getAuthor()).getPermissions(), commandEvent.getGuild())) {
            bot.getEventManager().handle(new CommandPermissionViolationEvent(commandEvent, command));
            return;
        }
        //Run command
        try {
            TextChannel channel = commandEvent.getChannel();
            //Send typing
            channel.sendTyping().queue();
            //Delete invoke message
            if (commandEvent.getGuild().getSelfMember().hasPermission(commandEvent.getChannel(), Permission.MESSAGE_MANAGE))
                commandEvent.getMessage().delete().queue();
            //Run the commands run method
            Result result = command.run(commandEvent.getArgs(), commandEvent);
            if (result != null)
                result.sendMessage(channel, 60);
            bot.getEventManager().handle(new CommandExecutedEvent(commandEvent, command));
        } catch (Exception e) {
            bot.getEventManager().handle(new CommandFailedEvent(commandEvent, command, e));
        }
    }

    /**
     * Registers  a command handler
     * @param commands The command handlers {@link me.schlaubi.votebot.core.command.Command}
     */
    public void registerCommands(Command... commands) {
        for (Command command : commands) {
            registerCommand(command);
        }
    }

    /**
     * Registers  a command handler
     * @param command The command handler {@link me.schlaubi.votebot.core.command.Command}
     */
    public void registerCommand(Command command) {
        for (String alias : command.getAliases()) {
            if (commandAssociations.containsKey(alias))
                log.warn(String.format("[CommandManager] Alias %s is already taken by %s", alias, commandAssociations.get(alias).getClass().getCanonicalName()));
            else
                commandAssociations.put(alias, command);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}