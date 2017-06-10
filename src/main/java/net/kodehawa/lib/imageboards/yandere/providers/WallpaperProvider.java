package net.kodehawa.lib.imageboards.yandere.providers;

import net.kodehawa.lib.imageboards.yandere.main.entities.Wallpaper;

@FunctionalInterface
public interface WallpaperProvider {
	void onSuccess(Wallpaper wallpaper);
}