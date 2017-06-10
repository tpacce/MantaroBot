package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.lib.imageboards.e621.e621;
import net.kodehawa.lib.imageboards.konachan.Konachan;
import net.kodehawa.lib.imageboards.rule34.Rule34;
import net.kodehawa.lib.imageboards.yandere.Yandere;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.image.YandereImageData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.OptsCmd.registerOption;

@Module
public class ImageCmds {

	public static final URLCache CACHE = new URLCache(20);
	private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
	private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random"; //this actualluy returns more questionable images than explicit tho
	private static final String[] responses = {"Aww, take a cat.", "%mention%, are you sad? ;w;, take a cat!", "You should all have a cat in your life, but a image will do.",
		"Am I cute yet?", "%mention%, I think you should have a cat."};
	private static e621 e621 = new e621();
	private static Konachan konachan = new Konachan();
	private static Rule34 rule34 = new Rule34();
	private static Yandere yandere = new Yandere();
	private static BidiMap<String, String> nRating = new DualHashBidiMap<>();
	private static Random r = new Random();
	private static String rating = "";

	@Command
	public static void cat(CommandRegistry cr) {
		cr.register("cat", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					String url = Unirest.get("http://random.cat/meow").asJsonAsync().get().getBody().getObject().get("file").toString();
					event.getChannel().sendFile(CACHE.getFile(url), "cat.jpg",
						new MessageBuilder().append(CollectionUtils.random(responses).replace("%mention%", event.getAuthor().getAsMention())).build()).queue();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Cat command")
					.setDescription("Sends a random cat image")
					.build();
			}
		});
	}

	@Command
	public static void catgirls(CommandRegistry cr) {
		cr.register("catgirl", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
				if (nsfw && !nsfwCheck(event, true, true)) return;

				try {
					JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
						.asJson()
						.getBody()
						.getObject();
					if (!obj.has("url")) {
						event.getChannel().sendMessage("Unable to find image").queue();
					} else {
						event.getChannel().sendFile(CACHE.getInput(obj.getString("url")), "catgirl.png", null).queue();
					}
				} catch (UnirestException e) {
					e.printStackTrace();
					event.getChannel().sendMessage("Unable to get image").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Catgirl command")
					.setDescription("**Sends catgirl images**")
					.addField("Usage", "`~>catgirl` - **Returns catgirl images.**" +
							"\n´`~>catgirl nsfw` - **Returns lewd or questionable cargirl images.**", false)
					.build();
			}
		});
	}

	@Command
	public static void e621(CommandRegistry cr) {
		cr.register("e621", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!nsfwCheck(event, true, true)) return;
				TextChannelGround.of(event).dropItemWithChance(13, 3);

				TextChannel channel = event.getChannel();

				if (args.length == 0) {
					onHelp(event);
					return;
				}

				channel.sendTyping().queue();
				String tags = content.toLowerCase().trim().replace(" ", "+");
				e621.onSearch(60, tags, (wallpaper) -> {
					if (wallpaper == null) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags.").queue();
						return;
					}
					String TAGS1 = wallpaper.getTags();

					EmbedBuilder builder = new EmbedBuilder();
					builder.setAuthor("Found image", wallpaper.getFile_url(), wallpaper.getFile_url())
						   .setDescription("Image uploaded by: " + (wallpaper.getAuthor() == null ? "not found" : wallpaper.getAuthor()))
						   .setImage(wallpaper.getFile_url())
						   .addField("Width", String.valueOf(wallpaper.getWidth()), true)
						   .addField("Height", String.valueOf(wallpaper.getHeight()), true)
						   .addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false)
						   .setFooter("If the image doesn't load, click the title.", null);

					channel.sendMessage(builder.build()).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "e621 commmand")
							   .setColor(Color.PINK)
							   .setDescription("**Retrieves images from the e621 image board.**")
							   .addField("Usage",
										 "`~>e621 <tags>` - **Gets an image based in the specified tags.**", false)
							   .addField("Parameters",
										 "`tags` - **Any valid image tag. For example animal_ears or no_bra.**", false)
							   .build();
			}
		});
	}

	@Command
	public static void kona(CommandRegistry cr) {
		cr.register("konachan", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				TextChannel channel = event.getChannel();

				if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("nsfw")) {
					onHelp(event);
					return;
				}

				String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
												.filter(channel1 -> channel1.equals(event.getChannel().getId())).findFirst().orElse(null);

				boolean nsfw = nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId()) || args.length > 0 && args[0].equalsIgnoreCase("nsfw");

				if (nsfw && !nsfwCheck(event, true, true)) return;
				if (nsfw) content = content.replace("nsfw ", "");

				channel.sendTyping().queue();
				String tags = content.toLowerCase().trim().replace(" ", "+");
				konachan.onSearch(nsfw, 60, tags, (wallpaper) -> {
					if (wallpaper == null) {
						if (!nsfw) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags or remove NSFW tags.").queue();
							return;
						}
						event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags.").queue();
						return;
					}
					String TAGS1 = wallpaper.getTags().stream().collect(Collectors.joining(", "));

					EmbedBuilder builder = new EmbedBuilder();
					builder.setAuthor("Found image", "https:" + wallpaper.getJpeg_url(), "https:" + wallpaper.getJpeg_url())
						   .setDescription("Image uploaded by: " + (wallpaper.getAuthor() == null ? "not found" : wallpaper.getAuthor()))
						   .setImage("https:" + wallpaper.getJpeg_url())
						   .addField("Width", String.valueOf(wallpaper.getWidth()), true)
						   .addField("Height", String.valueOf(wallpaper.getHeight()), true)
						   .addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false)
						   .setFooter("If the image doesn't load, click the title.", null);

					channel.sendMessage(builder.build()).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Konachan commmand")
							   .setColor(Color.PINK)
							   .setDescription("**Retrieves images from the Konachan image board.**")
							   .addField("Usage",
										 "`~>konachan nsfw <tags>` - **Gets an image based in the specified tags.**", false)
							   .addField("Parameters",
										 "`nsfw` - **Returns lewd or questionable konachan images.**\n"
										 + "`tags` - **Any valid image tag. For example animal_ears or no_bra.**", false)
							   .build();
			}
		});
	}

	@Command
	public static void boobs(CommandRegistry cr) {
		Random rand = new Random();

		cr.register("boobs", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!nsfwCheck(event, true, true)) return;
				TextChannel channel = event.getChannel();

				channel.sendTyping().queue();

				int randid = rand.nextInt(10765) + 1;

				JSONObject object = null;
				try {
					object = (JSONObject) Unirest.get("http://api.oboobs.ru/boobs/" + randid + "/1/rank/").asJsonAsync().get().getBody().getArray().get(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				String model = StringEscapeUtils.unescapeJava(object.get("model").toString()).replace("null", "not found");
				String preview = object.get("preview").toString();
				String id = object.get("id").toString();
				String rank = object.get("rank").toString();
				String author = StringEscapeUtils.unescapeJava(object.get("author").toString()).replace("null", "not found");

				EmbedBuilder builder = new EmbedBuilder();
				builder.setAuthor("Found image", "http://media.oboobs.ru/" + preview, "http://media.oboobs.ru/" + preview)
					   .setDescription("Image uploaded by: " + (author.length() == 0 ? "not found" : author))
					   .setImage("http://media.oboobs.ru/" + preview)
					   .addField("ID", "``" + id + "``", false)
					   .addField("Rank", rank, true)
					   .addField("Model", (model.length() == 0 ? "not found" : model), true)
					   .setFooter("If the image doesn't load, click the title.", null);

				channel.sendMessage(builder.build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Boobs command")
							   .setColor(Color.PINK)
							   .setDescription("Sends a random boobs image")
							   .build();
			}
		});

		cr.registerAlias("boobs", "boob");
		cr.registerAlias("boobs", "tit");
		cr.registerAlias("boobs", "tits");
	}

	@Command
	public static void ass(CommandRegistry cr) {
		Random rand = new Random();

		cr.register("ass", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!nsfwCheck(event, true, true)) return;
				TextChannel channel = event.getChannel();

				channel.sendTyping().queue();

				int randid = rand.nextInt(4758) + 1;

				JSONObject object = null;
				try {
					object = (JSONObject) Unirest.get("http://api.obutts.ru/butts/" + randid + "/1/rank/").asJsonAsync().get().getBody().getArray().get(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				String model = StringEscapeUtils.unescapeJava(object.get("model").toString()).replace("null", "not found");
				String preview = object.get("preview").toString();
				String id = object.get("id").toString();
				String rank = object.get("rank").toString();
				String author = StringEscapeUtils.unescapeJava(object.get("author").toString()).replace("null", "not found");

				EmbedBuilder builder = new EmbedBuilder();
				builder.setAuthor("Found image", "http://media.obutts.ru/" + preview, "http://media.obutts.ru/" + preview)
					   .setDescription("Image uploaded by: " + (author.length() == 0 ? "not found" : author))
					   .setImage("http://media.obutts.ru/" + preview)
					   .addField("ID", "``" + id + "``", false)
					   .addField("Rank", rank, true)
					   .addField("Model", (model.length() == 0 ? "not found" : model), true)
					   .setFooter("If the image doesn't load, click the title.", null);

				channel.sendMessage(builder.build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Ass command")
							   .setColor(Color.PINK)
							   .setDescription("Sends a random ass image")
							   .build();
			}
		});

		cr.registerAlias("ass", "nude");
		cr.registerAlias("ass", "nudes");
	}

	private static Random rand = new Random();

	public static void sendNudes(User user) {
		user.openPrivateChannel().queue((privateChannel1) -> {
			int randid = rand.nextInt(4758) + 1;

			JSONObject object = null;
			try {
				object = (JSONObject) Unirest.get("http://api.obutts.ru/butts/" + randid + "/1/rank/").asJsonAsync().get().getBody().getArray().get(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			String model = StringEscapeUtils.unescapeJava(object.get("model").toString()).replace("null", "not found");
			String preview = object.get("preview").toString();
			String id = object.get("id").toString();
			String rank = object.get("rank").toString();
			String author = StringEscapeUtils.unescapeJava(object.get("author").toString()).replace("null", "not found");

			EmbedBuilder builder = new EmbedBuilder();
			builder.setAuthor("Found image", "http://media.obutts.ru/" + preview, "http://media.obutts.ru/" + preview)
				   .setDescription("Image uploaded by: " + (author.length() == 0 ? "not found" : author))
				   .setImage("http://media.obutts.ru/" + preview)
				   .addField("ID", "``" + id + "``", false)
				   .addField("Rank", rank, true)
				   .addField("Model", (model.length() == 0 ? "not found" : model), true)
				   .setFooter("If the image doesn't load, click the title.", null);

			privateChannel1.sendMessage(builder.build()).queue();
		});
	}

	@Command
	public static void sendnudes(CommandRegistry cr) {
		cr.register("sendnudes", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Player user = MantaroData.db().getPlayer(event.getMember());
				if (!user.getNsfw()) {
					event.getMember().getUser().openPrivateChannel().queue((privateChannel) -> {
						privateChannel.sendMessage(EmoteReference.ERROR + "Hello, you sent message 'sendnudes' but you need to proof me something\n" +
												   "Type `18` to confirm you are at least 18.").queue();

						final User member = event.getMember().getUser();
						event.getJDA().addEventListener(new ListenerAdapter() {
							@Override
							public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
								if (event.getAuthor().getIdLong() == member.getIdLong() && event.getMessage().getContent().equals("18")) {
									Player user1 = MantaroData.db().getPlayer(event.getAuthor());
									user1.setNsfw(true);
									user1.save();
									event.getChannel().sendMessage(EmoteReference.POPPER + "You have confirmed you want nudes.").queue();
									sendNudes(member);
									event.getJDA().removeEventListener(this);
								}
							}
						});
					});
					return;
				}
				sendNudes(event.getMember().getUser());
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "SendNudes command")
							   .setColor(Color.PINK)
							   .setDescription("Sends a random nudes image to your private messages")
							   .build();
			}
		});

		cr.registerAlias("sendnudes", "sendnude");
	}

	@Command
	public static void rule34(CommandRegistry cr) {
		cr.register("rule34", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!nsfwCheck(event, true, true)) return;
				TextChannel channel = event.getChannel();

				if (args.length == 0) {
					onHelp(event);
					return;
				}

				channel.sendTyping().queue();
				String tags = content.toLowerCase().trim().replace(" ", "+");
				rule34.onSearch(60, tags, (wallpaper) -> {
					if (wallpaper == null) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags.").queue();
						return;
					}
					String TAGS1 = wallpaper.getTags();

					EmbedBuilder builder = new EmbedBuilder();
					builder.setAuthor("Found image", "https:" + wallpaper.getFile_url(), "https:" + wallpaper.getFile_url())
						   .setImage("https:" + wallpaper.getFile_url())
						   .addField("Width", String.valueOf(wallpaper.getWidth()), true)
						   .addField("Height", String.valueOf(wallpaper.getHeight()), true)
						   .addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false)
						   .setFooter("If the image doesn't load, click the title.", null);

					channel.sendMessage(builder.build()).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Rule34 commmand")
							   .setColor(Color.PINK)
							   .setDescription("**Retrieves images from the rule34 image board.**")
							   .addField("Usage",
										 "`~>rule34 <tags>` - **Gets an image based in the specified tags.**", false)
							   .addField("Parameters",
										 "`tags` - **Any valid image tag. For example animal_ears or no_bra.**", false)
							   .build();
			}
		});
	}

	@Command
	public static void yandere(CommandRegistry cr) {
		cr.register("yandere", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				TextChannel channel = event.getChannel();

				if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("nsfw")) {
					onHelp(event);
					return;
				}

				String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
												.filter(channel1 -> channel1.equals(event.getChannel().getId())).findFirst().orElse(null);

				boolean nsfw = nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId()) || args.length > 0 && args[0].equalsIgnoreCase("nsfw");

				if (nsfw && !nsfwCheck(event, true, true)) return;
				if (nsfw) content = content.replace("nsfw ", "");

				channel.sendTyping().queue();
				String tags = content.toLowerCase().trim().replace(" ", "+");
				yandere.onSearch(nsfw, 60, tags, (wallpaper) -> {
					if (wallpaper == null) {
						if (!nsfw) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags or remove NSFW tags.").queue();
							return;
						}
						event.getChannel().sendMessage(EmoteReference.ERROR + "**No results found**! Try with a fewer tags.").queue();
						return;
					}
					String TAGS1 = wallpaper.getTags().stream().collect(Collectors.joining(", "));

					EmbedBuilder builder = new EmbedBuilder();
					builder.setAuthor("Found image", wallpaper.getJpeg_url(), wallpaper.getJpeg_url())
						   .setDescription("Image uploaded by: " + (wallpaper.getAuthor() == null ? "not found" : wallpaper.getAuthor()))
						   .setImage(wallpaper.getJpeg_url())
						   .addField("Width", String.valueOf(wallpaper.getWidth()), true)
						   .addField("Height", String.valueOf(wallpaper.getHeight()), true)
						   .addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false)
						   .setFooter("If the image doesn't load, click the title.", null);

					channel.sendMessage(builder.build()).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Yandere commmand")
							   .setColor(Color.PINK)
							   .setDescription("**Retrieves images from the Yandere image board.**")
							   .addField("Usage",
										 "`~>yandere nsfw <tags>` - **Gets an image based in the specified tags.**", false)
							   .addField("Parameters",
										 "`nsfw` - **Returns lewd or questionable yandere images.**\n"
										 + "`tags` - **Any valid image tag. For example animal_ears or no_bra.**", false)
							   .build();
			}
		});
	}

	private static boolean nsfwCheck(GuildMessageReceivedEvent event, boolean isGlobal, boolean sendMessage) {
	    if(event.getChannel().isNSFW()) return true;

	    String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
			.filter(channel -> channel.equals(event.getChannel().getId())).findFirst().orElse(null);
		String rating1 = rating == null ? "s" : rating;
		boolean trigger = !isGlobal ? ((rating1.equals("s") || (nsfwChannel == null)) ? rating1.equals("s") : nsfwChannel.equals(event.getChannel().getId())) :
			nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId());

		if (!trigger) {
			if (sendMessage)
				event.getChannel().sendMessage(new EmbedBuilder().setDescription("Not on a NSFW channel. Cannot send lewd images.\n" +
						"**Reminder:** You can set this channel as NSFW by doing `~>opts nsfw toggle` if you are an administrator on this server.").build()).queue();
			return false;
		}

		return true;
	}

	@Command
	public static void onPostLoad(PostLoadEvent e) {
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");

		registerOption("nsfw:toggle", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			if (guildData.getGuildUnsafeChannels().contains(event.getChannel().getId())) {
				guildData.getGuildUnsafeChannels().remove(event.getChannel().getId());
				event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
				dbGuild.saveAsync();
				return;
			}

			guildData.getGuildUnsafeChannels().add(event.getChannel().getId());
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been enabled.").queue();
		});
	}
}
