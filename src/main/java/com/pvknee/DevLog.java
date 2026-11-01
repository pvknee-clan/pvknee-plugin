package com.pvknee;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.Text;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Reports what the plugin is doing: always at debug level, and in developer mode also as a console chat message
 * prefixed "[PvKnee]". Members never see the chat messages. Safe to call from any thread.
 */
@Slf4j
@Singleton
public class DevLog
{
	private static final String PREFIX = "[PvKnee] ";

	private final ChatMessageManager chatMessageManager;
	private final boolean developerMode;

	@Inject
	DevLog(ChatMessageManager chatMessageManager, @Named("developerMode") boolean developerMode)
	{
		this.chatMessageManager = chatMessageManager;
		this.developerMode = developerMode;
	}

	/**
	 * Logs a step such as "load: 5 pages". Takes {} placeholders like SLF4J; a trailing exception is logged with its
	 * stack trace but left out of the chat message.
	 */
	public void log(String format, Object... args)
	{
		FormattingTuple message = MessageFormatter.arrayFormat(format, args);
		log.debug(message.getMessage(), message.getThrowable());
		if (developerMode)
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.value(Text.escapeJagex(PREFIX + message.getMessage()))
				.build());
		}
	}
}
