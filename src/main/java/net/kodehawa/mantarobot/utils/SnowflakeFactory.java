package net.kodehawa.mantarobot.utils;

public class SnowflakeFactory {
	public class Generator {
		private final long datacenterId;
		private final long workerId;
		private long lastTimestamp = -1L;
		private long sequence = 0L;

		private Generator(long datacenterId, long workerId) {
			// sanity check for workerId
			if (workerId > maxWorkerId || workerId < 0) {
				throw new IllegalArgumentException(
					String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
			}
			if (datacenterId > maxDatacenterId || datacenterId < 0) {
				throw new IllegalArgumentException(
					String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
			}

			this.datacenterId = datacenterId;
			this.workerId = workerId;
		}

		public long getDatacenterId() {
			return datacenterId;
		}

		public SnowflakeFactory getFactory() {
			return SnowflakeFactory.this;
		}

		public long getWorkerId() {
			return workerId;
		}

		public long nextId() {
			long timestamp = timeGen();

			if (timestamp < lastTimestamp) {
				throw new IllegalStateException(String
					.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
						lastTimestamp - timestamp
					));
			}

			synchronized (this) {
				if (lastTimestamp == timestamp) {
					sequence = (sequence + 1) & sequenceMask;
					if (sequence == 0) timestamp = tilNextMillis(lastTimestamp);
				} else sequence = 0L;

				lastTimestamp = timestamp;

				return ((timestamp - epoch) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
			}
		}

		private long tilNextMillis(long lastTimestamp) {
			long timestamp = timeGen();
			while (timestamp <= lastTimestamp) timestamp = timeGen();
			return timestamp;
		}
	}

	public static final SnowflakeFactory
		TWITTER_FACTORY = new SnowflakeFactory(1288834974657L, 5, 5, 12),
		DISCORD_FACTORY = new SnowflakeFactory(1420070400000L, 5, 5, 12),
		MANTARO_FACTORY = new SnowflakeFactory(1494090000000L, 5, 5, 12);

	private final long datacenterIdBits;
	private final long datacenterIdShift;
	private final long epoch;
	private final long maxDatacenterId;
	private final long maxWorkerId;
	private final long sequenceBits;
	private final long sequenceMask;
	private final long timestampLeftShift;
	private final long workerIdBits;
	private final long workerIdShift;

	public SnowflakeFactory(long epoch, long datacenterIdBits, long workerIdBits, long sequenceBits) {
		this.datacenterIdBits = datacenterIdBits;
		this.sequenceBits = sequenceBits;
		this.epoch = epoch;
		this.workerIdBits = workerIdBits;

		datacenterIdShift = sequenceBits + workerIdBits;
		timestampLeftShift = datacenterIdShift + datacenterIdBits;
		workerIdShift = this.sequenceBits;
		sequenceMask = ~(-1L << this.sequenceBits);
		maxDatacenterId = ~(-1L << this.datacenterIdBits);
		maxWorkerId = ~(-1L << this.workerIdBits);
	}

	public long getCreationTime(long snowflake) {
		return (snowflake >> timestampLeftShift) + epoch;
	}

	public Generator getGenerator(long snowflake) {
		return getGenerator(snowflake >> datacenterIdShift, snowflake >> sequenceBits);
	}

	public Generator getGenerator(long datacenterId, long workerId) {
		return new Generator(datacenterId, workerId);
	}

	private long timeGen() {
		return System.currentTimeMillis();
	}
}
