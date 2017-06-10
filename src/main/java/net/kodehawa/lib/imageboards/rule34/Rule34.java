package net.kodehawa.lib.imageboards.rule34;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.lib.imageboards.rule34.entities.Hentai;
import net.kodehawa.lib.imageboards.rule34.providers.HentaiProvider;
import net.kodehawa.mantarobot.utils.Utils;
import us.monoid.web.Resty;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

@Slf4j
public class Rule34 {
	private final Resty resty = new Resty().identifyAsMozilla();
	private HashMap<String, Object> queryParams;

	private static final String BASEURL = "https://rule34.xxx/index.php?page=dapi&s=post&q=index";

	private static Random r = new Random();

	XMLInputFactory inputFactory = XMLInputFactory.newInstance();

	public Rule34() {
		queryParams = new HashMap<>();
	}

	private void get(final int page, final int limit, final String search, final HentaiProvider provider) {
		Async.thread("Image fetch thread", () -> {
			try {
				if (provider == null) throw new IllegalStateException("Provider is null");
				this.queryParams.put("limit", 1);
				Optional.ofNullable(search).ifPresent((element) -> {
					queryParams.put("tags", search.toLowerCase().trim());
					queryParams.remove("pid");
				});
				int maxPages;
				try {
					String response = this.resty.text(BASEURL + "&" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
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

					Hentai wallpaper = this.get(page1, limit, search, 1);
					Optional.ofNullable(search).ifPresent((s) -> {
						provider.onSuccess(wallpaper);
					});
				}
			} catch (Exception ignored) {
			}
		});
	}

	private Hentai get(int page, int limit, String search, int tryNumber) {
		if (tryNumber >= 3) {
			return null;
		}
		final int PAGE = page;
		page = r.nextInt(page) + 1;

		Hentai[] wallpaperss;
		try {
			int i = 0;
			String response = "<response success=\"false\" reason=\"Search error: API limited due to abuse.\"/>";
			while (response.equals("<response success=\"false\" reason=\"Search error: API limited due to abuse.\"/>")) {
				this.queryParams.put("limit", limit);
				this.queryParams.put("pid", page);
				Optional.ofNullable(search).ifPresent((element) -> {
					queryParams.put("tags", search.toLowerCase().trim());
				});
				queryParams.clear();
				response = this.resty.text(BASEURL + "&" + Utils.urlEncodeUTF8(this.queryParams).replace("%2B", "+")).toString();
				i++;
				if (i >= 3) {
					return null;
				}
			}
			wallpaperss = Utils.XML_MAPPER.readValue(response, Hentai[].class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			queryParams.clear();
		}
		List<Hentai> wallpapers = Arrays.asList(wallpaperss);
		if (wallpapers.isEmpty()) {
			return get(PAGE, limit, search, tryNumber + 1);
		}

		int number1 = r.nextInt(wallpapers.size() > 0 ? wallpapers.size() - 1 : wallpapers.size());
		return wallpapers.get(number1);
	}

	public void onSearch(int page, int limit, String search, HentaiProvider provider) {
		this.get(page, limit, search, provider);
	}
}
