package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.commands.base.Category;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpUtils {
	public static String forType(TextChannel channel, GuildData guildData, Category category) {
		return forType(
			CommandProcessor.REGISTRY.commands().entrySet().stream()
				.filter(entry -> entry.getValue().category() == category)
				.filter(entry -> !guildData.getDisabledCategories().contains(entry.getValue().category()))
				.filter(c -> !guildData.getDisabledCommands().contains(c.getKey()))
				.filter(c -> guildData.getChannelSpecificDisabledCommands().get(channel.getId()) == null || !guildData.getChannelSpecificDisabledCommands().get(channel.getId()).contains(c.getKey()))
				.map(Entry::getKey)
				.collect(Collectors.toList())
		);
	}

	public static String forType(List<String> values) {
		if(values.size() == 0) return "`Disabled`";

		return "``" + values.stream().sorted()
				.collect(Collectors.joining("`` ``")) + "``";
	}
}
