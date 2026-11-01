package com.pvknee.chat;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ChatColorConfig;

/**
 * Sends the plugin's chat messages with values in RuneLite's game message highlight colour (Chat Color settings),
 * the opaque or transparent one for the chatbox at the time. Used directly because RuneLite swaps its default
 * highlight for the game's own game message colour when one is set, which would make values look like the wording.
 * Safe to call from any thread.
 */
@Singleton
class ChatSender
{
	private final Client client;
	private final ClientThread clientThread;
	private final ChatColorConfig chatColors;
	private final ChatMessageManager chatMessageManager;

	@Inject
	ChatSender(Client client, ClientThread clientThread, ChatColorConfig chatColors, ChatMessageManager chatMessageManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.chatColors = chatColors;
		this.chatMessageManager = chatMessageManager;
	}

	void send(String template, Map<String, List<String>> values)
	{
		clientThread.invoke(() -> chatMessageManager.queue(Messages.message(template, values, highlight())));
	}

	private Color highlight()
	{
		boolean transparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) != 0;
		return transparent ? chatColors.transparentGameMessageHighlight() : chatColors.opaqueGameMessageHighlight();
	}
}
