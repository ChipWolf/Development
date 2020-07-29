package xyz.olivermartin.multichat.bungee.commands;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import xyz.olivermartin.multichat.bungee.ChatControl;
import xyz.olivermartin.multichat.bungee.CommandManager;
import xyz.olivermartin.multichat.bungee.ConfigManager;
import xyz.olivermartin.multichat.bungee.DebugManager;
import xyz.olivermartin.multichat.bungee.MessageManager;
import xyz.olivermartin.multichat.bungee.MultiChat;
import xyz.olivermartin.multichat.proxy.common.MultiChatProxy;
import xyz.olivermartin.multichat.proxy.common.channels.local.LocalChannel;
import xyz.olivermartin.multichat.proxy.common.channels.proxy.GlobalStaticProxyChannel;
import xyz.olivermartin.multichat.proxy.common.config.ConfigFile;
import xyz.olivermartin.multichat.proxy.common.contexts.GlobalContext;

/**
 * MultiChat (Admin) Command
 * <p>Used to view details about the plugin and display help information</p>
 * 
 * @author Oliver Martin (Revilo410)
 *
 */
public class MultiChatCommand extends Command {

	public MultiChatCommand() {
		super("multichat", "multichat.admin", (String[]) ConfigManager.getInstance().getHandler(ConfigFile.ALIASES).getConfig().getStringList("multichat").toArray(new String[0]));
	}

	private void displayHelp(CommandSender sender, int page) {

		switch (page) {

		case 1:

			MessageManager.sendMessage(sender, "command_multichat_help_1");
			break;

		case 2:

			MessageManager.sendMessage(sender, "command_multichat_help_2");
			break;

		default:

			MessageManager.sendMessage(sender, "command_multichat_help_3");
			break;

		}

	}

	public void execute(CommandSender sender, String[] args) {

		if (args.length < 1) {

			sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&2Multi&aChat &bVersion " + MultiChat.LATEST_VERSION)).create());
			sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&bBy Revilo410")).create());
			sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&bOriginally created for &3Oasis-MC.US")).create());
			sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&bUse &3/multichat help &bfor all commands")).create());

		} else {

			if (args.length == 1) {

				if (args[0].toLowerCase().equals("help")) {

					displayHelp(sender, 1);

				} else if (args[0].toLowerCase().equals("debug")) {

					DebugManager.toggle();
					DebugManager.log("Debug mode toggled");

				} else if (args[0].toLowerCase().equals("save")) {

					MessageManager.sendMessage(sender, "command_multichat_save_prepare");
					MultiChatProxy.getInstance().getFileStoreManager().save();
					MessageManager.sendMessage(sender, "command_multichat_save_completed");

				} else if (args[0].toLowerCase().equals("reload")) {

					MessageManager.sendMessage(sender, "command_multichat_reload_prepare");

					// Unregister commands
					((MultiChat)MultiChatProxy.getInstance().getPlugin()).unregisterCommands(ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig(), ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig());

					ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).startupConfig();
					MultiChat.configversion = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString("version");

					ConfigManager.getInstance().getHandler(ConfigFile.JOIN_MESSAGES).startupConfig();
					ConfigManager.getInstance().getHandler(ConfigFile.MESSAGES).startupConfig();
					ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).startupConfig();
					ConfigManager.getInstance().getHandler(ConfigFile.ALIASES).startupConfig();

					ConfigManager.getInstance().getRawHandler("messages_fr.yml").startupConfig();
					ConfigManager.getInstance().getRawHandler("joinmessages_fr.yml").startupConfig();
					ConfigManager.getInstance().getRawHandler("config_fr.yml").startupConfig();
					ConfigManager.getInstance().getRawHandler("chatcontrol_fr.yml").startupConfig();
					ConfigManager.getInstance().getRawHandler("aliases_fr.yml").startupConfig();

					// Reload, and re-register commands
					CommandManager.reload();
					((MultiChat)MultiChatProxy.getInstance().getPlugin()).registerCommands(ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig(), ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig());

					ChatControl.reload();

					System.out.println("VERSION LOADED: " + MultiChat.configversion);

					// Set up chat control stuff
					if (ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig().contains("link_control")) {
						ChatControl.controlLinks = ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig().getBoolean("link_control");
						ChatControl.linkMessage = ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig().getString("link_removal_message");
						if (ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig().contains("link_regex")) {
							ChatControl.linkRegex = ConfigManager.getInstance().getHandler(ConfigFile.CHAT_CONTROL).getConfig().getString("link_regex");
						}
					}

					if (ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().contains("privacy_settings")) {
						MultiChat.logPMs = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("privacy_settings").getBoolean("log_pms");
						MultiChat.logStaffChat = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("privacy_settings").getBoolean("log_staffchat");
						MultiChat.logGroupChat = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("privacy_settings").getBoolean("log_groupchat");
					}

					// Legacy servers for RGB approximation
					if (ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().contains("legacy_servers")) {
						MultiChat.legacyServers = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getStringList("legacy_servers");
					}

					// Set default channel
					String defaultChannel = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString("default_channel");
					boolean forceChannelOnJoin = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getBoolean("force_channel_on_join");

					// Set up global chat
					//GlobalAChannel channel = LegacyChannel.getGlobalChannel();
					//channel.setFormat(ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString("globalformat"));

					List<String> noGlobalServers = new ArrayList<String>();

					//channel.clearServers();
					// Add all appropriate servers to this hardcoded global chat stream
					for (String server : ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getStringList("no_global")) {
						//channel.addServer(server);
						noGlobalServers.add(server);
					}

					///

					// New context manager and channels
					GlobalContext globalContext = new GlobalContext(defaultChannel, forceChannelOnJoin, true, noGlobalServers);
					MultiChatProxy.getInstance().getContextManager().setGlobalContext(globalContext);

					MultiChatProxy.getInstance().getChannelManager().setGlobalChannel(new GlobalStaticProxyChannel("Global Channel", ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString("globalformat"), ConfigManager.getInstance().getHandler(ConfigFile.ALIASES).getConfig().getStringList("global"), MultiChatProxy.getInstance().getChannelManager()));
					MultiChatProxy.getInstance().getChannelManager().setLocalChannel(new LocalChannel("Local Channel", ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getString("globalformat"), ConfigManager.getInstance().getHandler(ConfigFile.ALIASES).getConfig().getStringList("local"), MultiChatProxy.getInstance().getChannelManager()));

					///

					if (ProxyServer.getInstance().getPluginManager().getPlugin("PremiumVanish") != null) {
						MultiChat.premiumVanish = true;

						if (ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().contains("premium_vanish")) {
							MultiChat.hideVanishedStaffInMsg = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("premium_vanish").getBoolean("prevent_message");
							MultiChat.hideVanishedStaffInStaffList = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("premium_vanish").getBoolean("prevent_staff_list");
							MultiChat.hideVanishedStaffInJoin = ConfigManager.getInstance().getHandler(ConfigFile.CONFIG).getConfig().getSection("premium_vanish").getBoolean("silence_join");
						}

					} else {
						MultiChat.premiumVanish = false;
					}

					MessageManager.sendMessage(sender, "command_multichat_reload_completed");
				}
			}

			if (args.length == 2) {

				if (args[0].toLowerCase().equals("help")) {

					if (args[1].toLowerCase().equals("1")) {
						displayHelp(sender,1);
					} else if (args[1].toLowerCase().equals("2")) {
						displayHelp(sender,2);
					} else {
						displayHelp(sender,3);
					}

				}
			}
		}
	}
}
