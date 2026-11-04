package com.pvknee.chat;

import com.pvknee.DevLog;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Competition;
import com.pvknee.data.Placing;
import com.pvknee.player.PlayerInfo;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Labels;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.util.Text;

/**
 * !sotw and !botw: when the member types one in chat, a game message only they see with the challenge, its time left
 * and their place and score. Others typing it get no reply from this plugin, and nothing is sent to the game.
 */
@Singleton
public class ChatCommands
{
	private static final String SOTW = "!sotw";
	private static final String BOTW = "!botw";

	private final ChatCommandManager chatCommandManager;
	private final ChatSender chatSender;
	private final ClanDataClient clanData;
	private final PlayerInfo playerInfo;
	private final DevLog devLog;

	@Inject
	ChatCommands(ChatCommandManager chatCommandManager, ChatSender chatSender, ClanDataClient clanData,
		PlayerInfo playerInfo, DevLog devLog)
	{
		this.chatCommandManager = chatCommandManager;
		this.chatSender = chatSender;
		this.clanData = clanData;
		this.playerInfo = playerInfo;
		this.devLog = devLog;
	}

	public void start()
	{
		chatCommandManager.registerCommand(SOTW, (chat, text) -> reply(chat, "skill", "cmd_sotw"));
		chatCommandManager.registerCommand(BOTW, (chat, text) -> reply(chat, "boss", "cmd_botw"));
	}

	public void stop()
	{
		chatCommandManager.unregisterCommand(SOTW);
		chatCommandManager.unregisterCommand(BOTW);
	}

	/**
	 * Runs on the client thread when anyone's chat message starts with the command; only the member's own get a reply.
	 */
	private void reply(ChatMessage chat, String type, String key)
	{
		PlayerState player = playerInfo.get();
		boolean own = chat.getType() == ChatMessageType.PRIVATECHATOUT || player.isNamed(Text.sanitize(chat.getName()));
		if (!own)
		{
			return;
		}

		devLog.log("command: {} typed", key);
		clanData.recent("command", data -> answer(data, player, type, key));
	}

	/**
	 * Fills the template from the standings in the loaded data; null if nothing could be loaded.
	 */
	private void answer(ClanData data, PlayerState player, String type, String key)
	{
		if (data == null)
		{
			devLog.log("command: no clan data");
			send("The clan data couldn't be loaded. Try again in a few minutes.", Map.of());
			return;
		}

		Competition competition = data.getCompetitions() == null ? null : data.getCompetitions().current(type);
		if (competition == null)
		{
			devLog.log("command: no current {} challenge", type);
			send("There's no " + ("boss".equals(type) ? "Boss" : "Skill") + " of the Week right now.", Map.of());
			return;
		}

		Map<String, List<String>> values = Messages.challengeValues(data);
		placeAndScore(values, Placing.find(Placing.board(competition), player::isNamed), type);
		send(Messages.template(data, key), values);
	}

	/**
	 * Fills {place}, {gain} and {you} (place and score together, e.g. "2nd, 1,942 xp") from the full standings, so a
	 * place below the table's cut-off still shows.
	 */
	private static void placeAndScore(Map<String, List<String>> values, Placing mine, String type)
	{
		if (mine == null)
		{
			values.put("place", Messages.one("not on the board yet"));
			values.put("gain", Messages.one("no gains yet"));
			values.put("you", Messages.one("skill".equals(type) ? "no xp yet" : "boss".equals(type) ? "no kills yet" : "no gains yet"));
			return;
		}

		String place = Labels.ordinal(mine.getPlace());
		String score = Labels.score(mine.getGained(), type);
		values.put("place", Messages.one(place));
		values.put("gain", Messages.one(score));
		values.put("you", Messages.one(place + ", " + score));
	}

	private void send(String template, Map<String, List<String>> values)
	{
		devLog.log("command: {}", Messages.plain(template, values));
		chatSender.send(template, values);
	}
}
