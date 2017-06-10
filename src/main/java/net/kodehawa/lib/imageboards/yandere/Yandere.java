package net.kodehawa.lib.imageboards.yandere;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.yandere.main.entities.Wallpaper;
import net.kodehawa.lib.imageboards.yandere.providers.WallpaperProvider;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import us.monoid.web.Resty;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Yandere {
	private final Resty resty = new Resty().identifyAsMozilla();
	private HashMap<String, Object> queryParams;

	private static final String BASEURL = "https://yande.re/";

	private static Random r = new Random();

	XMLInputFactory inputFactory = XMLInputFactory.newInstance();

	public Yandere() {
		queryParams = new HashMap<>();
	}

	private void get(boolean nsfw, final int limit, final String search, final WallpaperProvider provider) {
		Async.thread("Image fetch thread", () -> {
			try {
				if (provider == null) throw new IllegalStateException("Provider is null");
				this.queryParams.put("limit", 1);
				Optional.ofNullable(search).ifPresent((element) -> {
					queryParams.put("tags", search.toLowerCase().trim());
					queryParams.remove("page");
				});
				int maxPages;
				try {
					String response = this.resty.text(BASEURL + "post.xml?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
					XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(response));
					streamReader.nextTag();
					try {
						maxPages = Integer.parseInt(streamReader.getAttributeValue(0));
						maxPages = maxPages / limit;
					} catch (NumberFormatException e) {
						maxPages = 0;
					}
				} catch (Exception e) {
					maxPages = 0;
				} finally {
					queryParams.clear();
				}
				if (maxPages == 0) {
					provider.onSuccess(null);
				} else {
					Wallpaper wallpaper = this.get(nsfw, maxPages, limit, search, 1);
					Optional.ofNullable(search).ifPresent((s) -> {
						provider.onSuccess(wallpaper);
					});
				}
			} catch (Exception ignored) {
			}
		});
	}

	private Wallpaper get(boolean nsfw, int maxPages, int limit, String search, int tryNumber) {
		if (tryNumber >= 3) {
			return null;
		}
		int page = r.nextInt(maxPages) + 1;

		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		Optional.ofNullable(search).ifPresent((element) -> {
			queryParams.put("tags", search.toLowerCase().trim());
		});

		String response;
		try {
			response = this.resty.text(BASEURL + "post.json" + "?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			queryParams.clear();
		}
		Wallpaper[] wallpaperss = GsonDataManager.GSON_PRETTY.fromJson(response, Wallpaper[].class);

		List<Wallpaper> wallpapers = nsfw ? Arrays.asList(wallpaperss) : Arrays.stream(wallpaperss).filter((wallpaper ->
																													wallpaper.getRating().equalsIgnoreCase("s"))
		).collect(Collectors.toList());
		if (wallpapers.isEmpty()) {
			return get(nsfw, maxPages, limit, search, tryNumber + 1);
		}

		int number1 = r.nextInt(wallpapers.size() > 0 ? wallpapers.size() - 1 : wallpapers.size());
		return wallpapers.get(number1);
	}

	public void onSearch(boolean nsfw, int limit, String search, WallpaperProvider provider) {
		this.get(nsfw, limit, search, provider);
	}
}
