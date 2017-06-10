package net.kodehawa.lib.imageboards.e621;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.e621.main.entities.Furry;
import net.kodehawa.lib.imageboards.e621.providers.FurryProvider;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import us.monoid.web.Resty;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import com.rethinkdb.model.MapObject;

@Slf4j
public class e621 {
	private final Resty resty = new Resty().identifyAsMozilla();
	private HashMap<String, Object> queryParams;

	private static final String BASEURL = "https://e621.net/post/";

	private static Random r = new Random();

	XMLInputFactory inputFactory = XMLInputFactory.newInstance();

	public e621() {
		queryParams = new HashMap<>();
	}

	private void get(final int limit, final String search, final FurryProvider provider) {
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
					String response = this.resty.text(BASEURL + "index.xml?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
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
					Furry wallpaper = this.get(maxPages, limit, search, 1);
					Optional.ofNullable(search).ifPresent((s) -> {
						provider.onSuccess(wallpaper);
					});
				}
			} catch (Exception ignored) {
			}
		});
	}

	private Furry get(int maxPages, int limit, String search, int tryNumber) {
		if (tryNumber >= 3) {
			return null;
		}
		int page = r.nextInt(maxPages) + 1;

		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		Optional.ofNullable(search).ifPresent((element) -> {
			queryParams.put("tags", search.toLowerCase().trim());
		});

		Furry[] wallpaperss;
		try {
			String response = this.resty.text(BASEURL + "index.json?" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
			wallpaperss = GsonDataManager.GSON_PRETTY.fromJson(response, Furry[].class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			queryParams.clear();
		}
		List<Furry> wallpapers = Arrays.asList(wallpaperss);
		if (wallpapers.isEmpty()) {
			return get(maxPages, limit, search, tryNumber + 1);
		}

		int number1 = r.nextInt(wallpapers.size() > 0 ? wallpapers.size() - 1 : wallpapers.size());
		return wallpapers.get(number1);
	}

	public void onSearch(int limit, String search, FurryProvider provider) {
		this.get(limit, search, provider);
	}
}
