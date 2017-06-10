package net.kodehawa.lib.imageboards.yandere.providers;

@FunctionalInterface
public interface DownloadProvider {
	void onSuccess(String route);
}
