package net.kodehawa.dataporter;

import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;

import java.util.Map;
import java.util.Optional;

import static br.com.brjdevs.java.utils.strings.StringUtils.parse;
import static com.rethinkdb.RethinkDB.r;

public class MantaroDataPorter {
	public static void main(String[] args) {
		Map<String, Optional<String>> map = parse(args);

		//login
		String db = map.getOrDefault("d", Optional.empty()).orElse("mantaro");

		Connection c = r.connection()
			.hostname(map.getOrDefault("h", Optional.empty()).orElse("localhost"))
			.db(db)
			.connect();

		//renaming
		r.db(db).config().update(new MapObject<>("name", "old_" + db)).run(c);

		r.dbCreate(db).run(c);

		//tables
		r.tableCreate("commands").run(c);
		r.table("commands").indexCreate("guild").run(c);

		r.tableCreate("quotes").run(c);
		r.table("quotes").indexCreate("guild").run(c);

		r.tableCreate("marriages").run(c); //new
		r.table("marriages").indexCreate(
			"users",
			row -> r.array(row.g("user1"), row.g("user2"))
		).optArg("multi", true).run(c);

		r.table("marriages").indexCreate("tags").optArg("multi", true).run(c);

		r.tableCreate("mantaro").run(c);
		r.tableCreate("users").run(c);
		r.tableCreate("guilds").run(c);
		r.tableCreate("keys").run(c);

		r.table("commands").indexWait("guild").run(c);
		r.table("quotes").indexWait("guild").run(c);
		r.table("marriages").indexWait("users").run(c);

		//deleting
		r.dbDrop("old_" + db).run(c);
	}
}
