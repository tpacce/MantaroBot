package net.kodehawa.lib.imageboards.konachan.providers;

import net.kodehawa.lib.imageboards.konachan.main.entities.Wallpaper;

@FunctionalInterface
public interface WallpaperProvider {
	void onSuccess(Wallpaper wallpaper);
}