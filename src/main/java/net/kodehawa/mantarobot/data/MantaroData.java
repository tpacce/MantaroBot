package net.kodehawa.mantarobot.data;

import br.com.brjdevs.crossbot.currency.AbstractMoneyPacket;
import br.com.brjdevs.crossbot.currency.GetMoneyPacket;
import br.com.brjdevs.crossbot.currency.SetMoneyPacket;
import br.com.brjdevs.crossbot.currency.UpdateMoneyPacket;
import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.db.ManagedDatabase;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.data.ConnectionWatcherDataManager;
import net.kodehawa.mantarobot.utils.crossbot.CrossBotDataManager;
import net.kodehawa.mantarobot.utils.data.FunctionalPacketHandler;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.rethinkdb.RethinkDB.r;

public class MantaroData {
	private static GsonDataManager<Config> config;
	private static Connection conn;
	private static ConnectionWatcherDataManager connectionWatcher;
	private static CrossBotDataManager crossBot;
	private static ManagedDatabase db;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public static GsonDataManager<Config> config() {
		if (config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new);
		return config;
	}

	public static Connection conn() {
		Config c = config().get();
		if (conn == null) conn = r.connection().hostname(c.dbHost).port(c.dbPort).db(c.dbDb).connect();
		return conn;
	}

	public static ConnectionWatcherDataManager connectionWatcher() {
		if (connectionWatcher == null) {
			connectionWatcher = new ConnectionWatcherDataManager(MantaroBot.cwport);
		}
		return connectionWatcher;
	}

	public static CrossBotDataManager crossBot() {
		if (crossBot == null) {
			Config config = config().get();
			CrossBotDataManager.Builder builder;
			if (config.crossBotServer) {
				builder = new CrossBotDataManager.Builder(CrossBotDataManager.Builder.Type.SERVER);
			} else {
				builder = new CrossBotDataManager.Builder(CrossBotDataManager.Builder.Type.CLIENT)
                        .name("Mantaro")
                        .host(config.crossBotHost)
                        .secure(config.crossBotSSL);
			}
			crossBot = builder
                .password(config.crossBotPassword)
                .poolSize(config.crossBotThreads)
				.async(true, 10) //try to send everything on the queue every 10ms
				.port(config.crossBotPort)
                .addListeners((FunctionalPacketHandler)packet->{
                    if(packet instanceof AbstractMoneyPacket) {
                        long userid = ((AbstractMoneyPacket) packet).userid,
                             requestId = ((AbstractMoneyPacket) packet).requestId;
                        if(packet instanceof GetMoneyPacket) {
                            return new GetMoneyPacket.Response(userid, db().getGlobalPlayer(String.valueOf(userid)).getMoney(), requestId);
                        } else if(packet instanceof GetMoneyPacket.Response) {
                            crossBot.setResponse(((GetMoneyPacket.Response) packet).requestId, ((GetMoneyPacket.Response) packet).money);
                        } else if(packet instanceof UpdateMoneyPacket) {
                            long delta = ((UpdateMoneyPacket) packet).delta;
                            Player p = db().getGlobalPlayer(String.valueOf(userid));
                            if(delta < 0)
                                p.removeMoney(-delta);
                            else
                                p.addMoney(delta);
                            p.saveAsync();
                        } else if(packet instanceof SetMoneyPacket) {
                            Player p = db().getGlobalPlayer(String.valueOf(userid));
                            p.setMoney(((SetMoneyPacket) packet).money);
                            p.saveAsync();
                        }
                    }
                    return null;
                })
				.build();
		}

		return crossBot;
	}

	public static ManagedDatabase db() {
		if (db == null) db = new ManagedDatabase(conn());
		return db;
	}

	public static ScheduledExecutorService getExecutor() {
		return exec;
	}

	public static void queue(Callable<?> action) {
		getExecutor().submit(action);
	}

	public static void queue(Runnable runnable) {
		getExecutor().submit(runnable);
	}
}
