package net.kodehawa.mantarobot.commands.utils;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Szymo on 10.06.2017.
 */
public class RemindMeData {

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private TextChannel channel;
    private User user;
    private long when;
    private String about;

    private ScheduledFuture runnable;

    public RemindMeData(TextChannel channel, User user, long when, String about) {
        this.channel = channel;
        this.user = user;
        this.when = when;
        this.about = about;

        this.runnable = scheduler.schedule(() -> this.channel.sendMessage("#RemindMe " + user.getAsMention() + " " + about).queue(), this.when, TimeUnit.MILLISECONDS);
    }

    public User getUser() {
        return this.user;
    }

    public long getWhen() {
        return this.when;
    }

    public String getAbout() {
        return this.about;
    }

    public void cancel() {
        runnable.cancel(true);
    }
}
