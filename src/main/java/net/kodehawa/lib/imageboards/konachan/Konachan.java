package net.kodehawa.lib.imageboards.konachan;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.konachan.main.entities.Wallpaper;
import net.kodehawa.lib.imageboards.konachan.providers.WallpaperProvider;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import us.monoid.web.Resty;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Konachan {
	private final Resty resty = new Resty().identifyAsMozilla();
	private HashMap<String, Object> queryParams;

	private static final String BASEURL = "https://konachan.net/";
	private static final String NSFWURL = "https://konachan.com/";

	private static Random r = new Random();

	XMLInputFactory inputFactory = XMLInputFactory.newInstance();

	public Konachan() {
		queryParams = new HashMap<>();
	}

	private void get(boolean nsfw, final int page, final int limit, final String search, final WallpaperProvider provider) {
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
					String url = nsfw ? NSFWURL : BASEURL;
					String response = this.resty.text(url + "post.xml" + "?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
					XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(response));
					streamReader.nextTag();
					try {
						maxPages = Integer.parseInt(streamReader.getAttributeValue(0));
						maxPages = maxPages / 60;
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
					int page1 = maxPages;

					Wallpaper wallpaper = this.get(nsfw, page1, limit, search, 1);
					Optional.ofNullable(search).ifPresent((s) -> {
						provider.onSuccess(wallpaper);
					});
				}
			} catch (Exception ignored) {
			}
		});
	}

	private Wallpaper get(boolean nsfw, int page, int limit, String search, int tryNumber) {
		if (tryNumber >= 3) {
			return null;
		}
		final int PAGE = page;
		page = r.nextInt(page) + 1;

		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		Optional.ofNullable(search).ifPresent((element) -> {
			queryParams.put("tags", search.toLowerCase().trim());
		});

		String response;
		try {
			String url = nsfw ? NSFWURL : BASEURL;
			response = this.resty.text(url + "post.json" + "?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
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
			return get(nsfw, PAGE, limit, search, tryNumber + 1);
		}

		int number1 = r.nextInt(wallpapers.size() > 0 ? wallpapers.size() - 1 : wallpapers.size());
		return wallpapers.get(number1);
	}

	public void onSearch(boolean nsfw, int page, int limit, String search, WallpaperProvider provider) {
		this.get(nsfw, page, limit, search, provider);
	}
}
