package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.async.Async;
import br.com.brjdevs.java.utils.texts.StringUtils;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.music.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.utils.RemindMeData;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class FunCmds {

	private static Random r = new Random();

	private static List<RemindMeData> reminders = new ArrayList<RemindMeData>();

	@Command
	public static void remindme(CommandRegistry registry) {
		registry.register("remindme", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length < 2) {
				    if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
                        List<RemindMeData> userReminders = reminders.stream().filter(reminder -> reminder.getUser().getIdLong() == (event.getAuthor().getIdLong())).collect(Collectors.toList());
                        if (userReminders.size() == 0) {
                            event.getChannel().sendMessage("You don't have any reminders.").queue();
                            return;
                        }
                        userReminders.forEach(remindMeData -> remindMeData.cancel());
                        userReminders.forEach(remindMeData -> reminders.remove(remindMeData));
                        event.getChannel().sendMessage("All your reminders were removed.").queue();
                        return;
                    }
					onHelp(event);
					return;
				}

				final long time = AudioCmdUtils.parseTime(args[0]);
				final String about = content.replace(args[0] + " ", "");

                reminders.add(new RemindMeData(event.getChannel(), event.getAuthor(), time, about));

                event.getChannel().sendMessage("I will remind you with " + about + " in " + args[0]).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "RemindMe command")
							   .setDescription(
									   "Reminds you after x time about y\n" +
									   "`~>remindme [time] [about]`: Reminds you after x about y."
							   )
							   .build();
			}
		});
	}

	@Command
	public static void coinflip(CommandRegistry cr) {
		cr.register("coinflip", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				int times;
				if (args.length == 0 || content.length() == 0) times = 1;
				else {
					try {
						times = Integer.parseInt(args[0]);
						if (times > 1000) {
							event.getChannel().sendMessage(
								EmoteReference.ERROR + "Whoah there! The limit is 1,000 coinflips").queue();
							return;
						}
					} catch (NumberFormatException nfe) {
						event.getChannel().sendMessage(
							EmoteReference.ERROR + "You need to specify an Integer for the amount of " +
								"repetitions").queue();
						return;
					}
				}

				final int[] heads = {0};
				final int[] tails = {0};
				doTimes(times, () -> {
					if (new Random().nextBoolean()) heads[0]++;
					else tails[0]++;
				});
				String flips = times == 1 ? "time" : "times";
				event.getChannel().sendMessage(
					EmoteReference.PENNY + " Your result from **" + times + "** " + flips + " yielded " +
						"**" + heads[0] + "** heads and **" + tails[0] + "** tails").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Coinflip command")
					.setDescription("**Flips a coin with a defined number of repetitions**")
					.addField("Usage", "`~>coinflip <number of times>` - **Flips a coin x number of times**", false)
					.build();
			}
		});
	}

	@Command
	public static void marry(CommandRegistry cr) {
		cr.register("marry", new SimpleCommand(Category.FUN) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 0) {
					onError(event);
					return;
				}

				if (args[0].equals("divorce")) {
					Player user = MantaroData.db().getPlayer(event.getMember());

					if (user.getData().getMarriedWith() == null) {
						event.getChannel().sendMessage(
							EmoteReference.ERROR + "You aren't married with anyone, why don't you get started?")
							.queue();
						return;
					}

					User user1 = user.getData().getMarriedWith() == null
						? null : MantaroBot.getInstance().getUserById(user.getData().getMarriedWith());

					if (user1 == null) {
						user.getData().setMarriedWith(null);
						user.getData().setMarriedSince(0L);
						user.saveAsync();
						event.getChannel().sendMessage(
							EmoteReference.CORRECT + "Now you're single. I guess that's nice?").queue();
						return;
					}

					Player marriedWith = MantaroData.db().getPlayer(user1);

					marriedWith.getData().setMarriedWith(null);
					marriedWith.getData().setMarriedSince(0L);
					marriedWith.saveAsync();

					user.getData().setMarriedWith(null);
					user.getData().setMarriedSince(0L);
					user.saveAsync();
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. I guess that's nice?")
						.queue();
					return;
				}


				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Mention the user you want to marry with.")
						.queue();
					return;
				}

				User member = event.getAuthor();
				User user = event.getMessage().getMentionedUsers().get(0);
				Player player = MantaroData.db().getPlayer(event.getMember());
				User user1 = player.getData().getMarriedWith() == null
						? null : MantaroBot.getInstance().getUserById(player.getData().getMarriedWith());

				if (user.getId().equals(event.getAuthor().getId())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry with yourself.").queue();
					return;
				}

				if (user.isBot()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry a bot.").queue();
					return;
				}

				if (player.getData().isMarried()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "That user is married already.").queue();
					return;
				}

				if (player.getData().isMarried() && user1 != null) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You are married already.").queue();
					return;
				}

				if (InteractiveOperations.create(
					event.getChannel(), "Marriage Proposal", (int) TimeUnit.SECONDS.toMillis(120), OptionalInt.empty(),
					(e) -> {
						if (!e.getAuthor().getId().equals(user.getId())) return false;

						if (e.getMessage().getContent().equalsIgnoreCase("yes")) {
							Player user11 = MantaroData.db().getPlayer(e.getMember());
							Player marry = MantaroData.db().getPlayer(e.getGuild().getMember(member));
							user11.getData().setMarriedWith(member.getId());
							marry.getData().setMarriedWith(e.getAuthor().getId());
							e.getChannel().sendMessage(EmoteReference.POPPER + e.getMember()
								.getEffectiveName() + " accepted the proposal of " + member.getName() + "!").queue();
							user11.save();
							marry.save();
							return true;
						}

						if (e.getMessage().getContent().equalsIgnoreCase("no")) {
							e.getChannel().sendMessage(EmoteReference.CORRECT + "Denied proposal.").queue();
							return true;
						}

						return false;
					}
				)) {
					TextChannelGround.of(event).dropItemWithChance(Items.LOVE_LETTER, 2);
					event.getChannel().sendMessage(EmoteReference.MEGA + user
						.getName() + ", respond with **yes** or **no** to the marriage proposal from " + event
						.getAuthor().getName() + ".").queue();

				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Marriage command")
					.setDescription("**Basically marries you with a user.**")
					.addField("Usage", "`~>marry <@mention>` - **Propose to someone**", false)
					.addField(
						"Divorcing", "Well, if you don't want to be married anymore you can just do `~>marry divorce`",
						false
					)
					.build();
			}
		});
	}

	@Command
	public static void ratewaifu(CommandRegistry cr) {
		cr.register("ratewaifu", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

				if (args.length == 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Give me a waifu to rate!").queue();
					return;
				}

				int waifuRate = r.nextInt(100);
				if (content.equalsIgnoreCase("mantaro")) waifuRate = 100;

				event.getChannel().sendMessage(
					EmoteReference.THINKING + "I rate " + content + " with a **" + waifuRate + "/100**").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Rate your waifu")
					.setDescription("**Just rates your waifu from zero to 100. Results may vary.**")
					.build();
			}
		});

		cr.registerAlias("ratewaifu", "rw");
	}

	@Command
	public static void roll(CommandRegistry registry) {
		registry.register("roll", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Map<String, Optional<String>> opts = StringUtils.parse(args);

				int size = 6, amount = 1;

				if (opts.containsKey("size")) {
					try {
						size = Integer.parseInt(opts.get("size").orElse(""));
					} catch (Exception ignored) {}
				}

				if (opts.containsKey("amount")) {
					try {
						amount = Integer.parseInt(opts.get("amount").orElse(""));
					} catch (Exception ignored) {}
				} else if (opts.containsKey(null)) { //Backwards Compatibility
					try {
						amount = Integer.parseInt(opts.get(null).orElse(""));
					} catch (Exception ignored) {}
				}

				if (amount >= 100) amount = 100;
				event.getChannel().sendMessage(
						EmoteReference.DICE + "You got **" + diceRoll(size, amount) + "**" +
						(amount == 1 ? "!" : (", doing **" + amount + "** rolls."))
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Dice command")
							   .setDescription(
									   "Roll a any-sided dice a 1 or more times\n" +
									   "`~>roll [-amount <number>] [-size <number>]`: Rolls a dice of the specified size the specified times.\n" +
									   "(By default, this command will roll a 6-sized dice 1 time.)"
							   )
							   .build();
			}
		});
	}

	private static long diceRoll(int size, int amount) {
		long sum = 0;
		for (int i = 0; i < amount; i++) sum += r.nextInt(size) + 1;
		return sum;
	}
}
