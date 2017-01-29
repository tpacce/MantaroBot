package net.kodehawa.mantarobot.commands;


import bsh.Interpreter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class OwnerCmds extends Module {
	public static GuildMessageReceivedEvent tempEvt = null;
	private static Logger LOGGER = LoggerFactory.getLogger("Owner");

	public OwnerCmds() {
		super(Category.MODERATION);
		add();
		eval();
		shutdown();
	}

	private void add() {
		super.register("varadd", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				switch (args[0]) {
					case "pat":
						MantaroData.getPatting().get().add(args[1]);
						MantaroData.getPatting().update();
						event.getChannel().sendMessage("Added to pat list: " + args[1]).queue();
						break;
					case "hug":
						MantaroData.getHugs().get().add(args[1]);
						MantaroData.getHugs().update();
						event.getChannel().sendMessage("Added to hug list: " + args[1]).queue();
						break;
					case "greeting":
						MantaroData.getGreeting().get().add(content.replace(args[0], ""));
						MantaroData.getGreeting().update();
						event.getChannel().sendMessage("Added to greet list: " + content.replace(args[0], "")).queue();
						break;
					case "splash":
						MantaroData.getSplashes().get().add(content.replace(args[0], ""));
						MantaroData.getSplashes().update();
						event.getChannel().sendMessage("Added to splash list: " + content.replace(args[0], "")).queue();
						break;
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Add to list command")
					.setDescription("Adds a parameter to a list."
						+ "\n Arguments: \n pat <args[1]>, hug <args[1]>, greeting <content>, splash <content>")
						.build();
			}
		});
	}

	private void eval(){
		super.register("eval", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!MantaroData.getConfig().get().owners.contains(event.getAuthor().getId())) {
					return;
				}

				if(args[0].equals("js")){
					ScriptEngine script = new ScriptEngineManager().getEngineByName("nashorn");
					script.put("jda", event.getJDA());
					script.put("event", event);
					script.put("guild", event.getGuild());
					script.put("channel", event.getChannel());
					String evalString = content.replaceAll(args[0], "");
					Object toSend;
					EmbedBuilder embedBuilder = new EmbedBuilder().setAuthor("Eval", null, event.getAuthor().getAvatarUrl()).setFooter("Asked by " + event.getAuthor().getName(), null);
					try {
						script.eval("imports = new JavaImporter(java.util, java.io, java.net)\n");
						toSend = script.eval("(function() {" +
								"with(imports) {"
								+ evalString + "\n}" +
								"})()");
					} catch (ScriptException e) {
						e.printStackTrace(); //TODO LOG THAT SHIT
						toSend = e.getMessage();
						embedBuilder.setDescription(toSend.toString());
					}
					String out = toSend == null ? "Executed with no errors and no returns" : toSend.toString();
					embedBuilder.setDescription(out);



					event.getChannel().sendMessage(embedBuilder.build()).queue();
					return;
				}

				tempEvt = event;
				try {
					Interpreter interpreter = new Interpreter();
					String evalHeader =
							"import *; "
									+ "private JDAImpl jda = MantaroBot.getJDA(); "
									+ "private GuildMessageReceivedEvent evt = net.kodehawa.mantarobot.commands.OwnerCmds.tempEvt;";
					Object toSendTmp = interpreter.eval(evalHeader + content);
					EmbedBuilder embed = new EmbedBuilder();
					String toSend = toSendTmp == null ? "Executed successfully with no objects returned" : toSendTmp.toString();
					embed.setAuthor("Executed eval with success", null, event.getAuthor().getAvatarUrl())
							.setDescription("Returned: " + toSend)
							.setFooter("Asked by: " + event.getAuthor().getName(), null);
					event.getChannel().sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("Code evaluation returned ``" + e.getClass().getSimpleName() + "``, with cause" +
							" ``" + e.getCause() + "``").queue();
					LOGGER.warn("Problem evaluating code!", e);
					e.printStackTrace(); //TODO LOG THAT SHIT
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Eval command")
						.setDescription("Guess what, it evals.")
						.build();
			}
		});
	}

	private void shutdown() {
		super.register("shutdown", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!MantaroData.getConfig().get().owners.contains(event.getAuthor().getId())) {
					event.getChannel().sendMessage("Seems like you cannot do that, you silly <3").queue();
					return;
				}

				try {
					shutdown(event);
				} catch (Exception e) {
					LOGGER.warn("Couldn't shut down." + e.toString(), e);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Shutdown")
					.setDescription("Shutdowns the bot.")
						.build();
			}
		});
	}

	private synchronized void shutdown(GuildMessageReceivedEvent event) {
		MantaroBot.getJDA().getRegisteredListeners().forEach(listener -> MantaroBot.getJDA().removeEventListener(listener));
		System.gc();
		event.getChannel().sendMessage("*goes to sleep*").queue();
		System.exit(0);
	}
}