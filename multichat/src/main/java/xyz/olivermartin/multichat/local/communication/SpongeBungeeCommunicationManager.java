package xyz.olivermartin.multichat.local.communication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;

import xyz.olivermartin.multichat.local.MultiChatLocalPlatform;

/**
 * Allows MultiChatLocal running on Sponge to communicate with a Bungeecord Proxy
 * 
 * <p>PLEASE NOTE: The RawDataChannels on Sponge MUST be registered with this communication manager before it will work!</p>
 * 
 * @author Oliver Martin (Revilo410)
 *
 */
public class SpongeBungeeCommunicationManager extends LocalBungeeCommunicationManager {

	private Map<String, RawDataChannel> channels;

	protected SpongeBungeeCommunicationManager() {
		super(MultiChatLocalPlatform.SPONGE);
		channels = new HashMap<String, RawDataChannel>();
	}

	public void registerChannel(String channelName, RawDataChannel channel) {
		this.channels.put(channelName, channel);
	}

	public void unregisterChannel(String channelName) {
		if (this.channels.containsKey(channelName)) this.channels.remove(channelName);
	}

	@Override
	protected boolean sendUUIDAndString(String channel, UUID uuid, String value) {

		if (!this.channels.containsKey(channel)) throw new IllegalStateException("Sponge Raw Data Channels must first be registered with MultiChat's SpongeBungeeCommunicationManager!");

		if (Sponge.getServer().getOnlinePlayers().size() < 1) return false;

		Player facilitatingPlayer = (Player) Sponge.getServer().getOnlinePlayers().toArray()[0];

		this.channels.get(channel).sendTo(facilitatingPlayer, buffer -> buffer.writeUTF(uuid.toString()).writeUTF(value));

		return true;
	}

}
