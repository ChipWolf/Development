package xyz.olivermartin.multichat.bungee.commands;

import java.util.Optional;

import com.olivermartin410.plugins.TGroupChatInfo;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.olivermartin.multichat.bungee.ChatControl;
import xyz.olivermartin.multichat.bungee.ChatManipulation;
import xyz.olivermartin.multichat.bungee.ConfigManager;
import xyz.olivermartin.multichat.bungee.ConsoleManager;
import xyz.olivermartin.multichat.bungee.Events;
import xyz.olivermartin.multichat.bungee.MessageManager;
import xyz.olivermartin.multichat.bungee.MultiChat;
import xyz.olivermartin.multichat.common.MultiChatUtil;
import xyz.olivermartin.multichat.proxy.common.MultiChatProxy;
import xyz.olivermartin.multichat.proxy.common.ProxyJsonUtils;
import xyz.olivermartin.multichat.proxy.common.config.ConfigFile;
import xyz.olivermartin.multichat.proxy.common.config.ConfigValues;
import xyz.olivermartin.multichat.proxy.common.storage.ProxyDataStore;

/**
 * Group Chat Messaging Command
 * <p>Allows players to send a message direct to a group chat or toggle group chats</p>
 *
 * @author Oliver Martin (Revilo410)
 */
public class GCCommand extends Command {

    public GCCommand() {
        super("mcgc", "multichat.group", ConfigManager.getInstance().getHandler(ConfigFile.ALIASES).getConfig().getStringList("gc").toArray(new String[0]));
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            MessageManager.sendMessage(sender, args.length == 0
                    ? "command_gc_only_players_toggle"
                    : "command_gc_only_players_speak"
            );
            return;
        }

        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;
        if (args.length == 0) {
            boolean toggleResult = Events.toggleGC(proxiedPlayer.getUniqueId());
            MessageManager.sendMessage(sender, "command_gc_toggle_" + (toggleResult ? "on" : "off"));
            return;
        }

        ProxyDataStore proxyDataStore = MultiChatProxy.getInstance().getDataStore();
        String viewedChat = proxyDataStore.getViewedChats().get(proxiedPlayer.getUniqueId());
        if (viewedChat == null) {
            MessageManager.sendMessage(sender, "command_gc_no_chat_selected");
            return;
        }

        TGroupChatInfo groupChatInfo = proxyDataStore.getGroupChats().get(viewedChat);
        if (groupChatInfo == null) {
            MessageManager.sendMessage(sender, "command_gc_no_longer_exists");
            return;
        }

        String playerName = sender.getName();

        if (groupChatInfo.getFormal() && groupChatInfo.getAdmins().contains(proxiedPlayer.getUniqueId())) {
            playerName = "&o" + playerName;
        }

        sendMessage(String.join(" ", args), playerName, groupChatInfo);
    }

    public static void sendMessage(String originalMessage, String playerName, TGroupChatInfo groupInfo) {
        ProxyDataStore proxyDataStore = MultiChatProxy.getInstance().getDataStore();
        ChatManipulation manipulation = new ChatManipulation();

        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerName);
        if (proxiedPlayer != null) {
            if (ChatControl.isMuted(proxiedPlayer.getUniqueId(), "group_chats")) {
                MessageManager.sendMessage(proxiedPlayer, "mute_cannot_send_message");
                return;
            }

            if (ChatControl.handleSpam(proxiedPlayer, originalMessage, "group_chats"))
                return;
        }

        Optional<String> optionalChatRules;

        optionalChatRules = ChatControl.applyChatRules(originalMessage, "group_chats", playerName);

        if (!optionalChatRules.isPresent())
            return;
        originalMessage = optionalChatRules.get();

        String messageFormat = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString(ConfigValues.Config.GroupChat.FORMAT);
        String translatedMessage = MultiChatUtil.translateColourCodes(
                manipulation.replaceGroupChatVars(messageFormat, playerName, originalMessage, groupInfo.getName())
        );
        String translatedOriginalMessage = MultiChatUtil.translateColourCodes(originalMessage);

        BaseComponent[] modernMessage = ProxyJsonUtils.parseMessage(translatedMessage,
                "%MESSAGE%",
                translatedOriginalMessage
        );

        BaseComponent[] legacyMessage = ProxyJsonUtils.parseMessage(
                MultiChatUtil.approximateRGBColourCodes(translatedMessage),
                "%MESSAGE%",
                MultiChatUtil.approximateRGBColourCodes(translatedOriginalMessage)
        );

        ProxyServer.getInstance().getPlayers().stream()
                .filter(target -> target.getServer() != null
                        && (groupInfo.existsViewer(target.getUniqueId()) && target.hasPermission("multichat.group"))
                        || proxyDataStore.getAllSpy().contains(target.getUniqueId())
                )
                .forEach(target -> {
                    if (proxiedPlayer != null
                            && ChatControl.ignores(proxiedPlayer.getUniqueId(), target.getUniqueId(), "group_chats")) {
                        ChatControl.sendIgnoreNotifications(target, proxiedPlayer, "group_chats");
                        return;
                    }

                    // TODO: Move legacy check inside parsing at some point
                    if (MultiChat.legacyServers.contains(target.getServer().getInfo().getName())) {
                        target.sendMessage(legacyMessage);
                        return;
                    }
                    target.sendMessage(modernMessage);
                });

        StringBuilder consoleMessage = new StringBuilder();
        for (BaseComponent bc : legacyMessage)
            consoleMessage.append(bc.toLegacyText());
        ConsoleManager.logGroupChat(consoleMessage.toString());
    }
}
