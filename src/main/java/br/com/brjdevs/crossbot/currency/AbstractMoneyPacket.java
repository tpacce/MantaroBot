package br.com.brjdevs.crossbot.currency;

public abstract class AbstractMoneyPacket {
	public final long userid;
	public final long requestId;

	AbstractMoneyPacket(long userid, long requestId) {
		this.userid = userid;
		this.requestId = requestId;
	}
}
