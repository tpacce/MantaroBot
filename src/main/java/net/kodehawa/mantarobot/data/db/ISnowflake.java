package net.kodehawa.mantarobot.data.db;

import net.kodehawa.mantarobot.utils.SnowflakeFactory;

import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Marks a snowflake entity. Snowflake entities are ones that have an id that uniquely identifies them.
 * <br>(Yes, this Interface was blalantly copied from JDA)
 */
public interface ISnowflake {
	static ISnowflake convert(net.dv8tion.jda.core.entities.ISnowflake snowflake) {
		return new ISnowflake() {
			@Override
			public long getId() {
				return snowflake.getIdLong();
			}

			@Override
			public SnowflakeFactory getFactory() {
				return SnowflakeFactory.DISCORD_FACTORY;
			}
		};
	}

	/**
	 * The Snowflake id of this entity. This is unique to every entity and will never change.
	 *
	 * @return long containing the Id.
	 */
	long getId();

	/**
	 * The time this entity was created. Calculated through the Snowflake in {@link #getId}.
	 *
	 * @return OffsetDateTime - Time this entity was created at.
	 */
	default OffsetDateTime getCreationTime() {
		Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		gmt.setTimeInMillis(getFactory().getCreationTime(getId()));
		return OffsetDateTime.ofInstant(gmt.toInstant(), gmt.getTimeZone().toZoneId());
	}

	/**
	 * The Factory that made this Snowflake. Can be customized to reflect the value on {@link #getCreationTime}
	 *
	 * @return an Snowflake Factory.
	 */
	default SnowflakeFactory getFactory() {
		return SnowflakeFactory.MANTARO_FACTORY;
	}
}
