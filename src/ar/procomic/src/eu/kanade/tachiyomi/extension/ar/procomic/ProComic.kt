package eu.kanade.tachiyomi.extension.ar.procomic

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ProComic : ParsedHttpSource() {
    override val name = "ProComic"
    override val baseUrl = "https://procomic.pro"
    override val lang = "ar"
    override val supportsLatest = true
    override val client = network.cloudflareClient

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/series?page=$page&sort=views", headers)

    override fun popularMangaSelector(): String = "div.series-card, article"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h3, h2")?.text() ?: ""
        setUrlWithoutDomain(element.selectFirst("a")?.attr("href") ?: "")
        thumbnail_url = element.selectFirst("img")?.attr("src")
    }

    override fun popularMangaNextPageSelector(): String = "a[rel=next]"

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/series?page=$page&sort=latest", headers)

    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/search?q=$query&page=$page", headers)

    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1")?.text() ?: ""
        thumbnail_url = document.selectFirst("img.cover")?.attr("src")
        author = document.selectFirst(".author")?.text()
        description = document.selectFirst(".synopsis")?.text()
        genre = document.select(".tags a").joinToString(", ") { it.text() }
        status = when (document.selectFirst(".status")?.text()?.trim()) {
            "مستمر" -> SManga.ONGOING
            "مكتمل" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    override fun chapterListSelector(): String = ".chapter-item"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.selectFirst("a")?.attr("href") ?: "")
        name = element.selectFirst("a")?.text() ?: ""
        chapter_number = name.substringAfterLast(" ").toFloatOrNull() ?: -1f
    }

    override fun pageListParse(document: Document): List<Page> =
        document.select("img[src*='/uploads/']")
            .mapIndexed { i, img -> Page(i, "", img.attr("abs:src")) }

    override fun imageUrlParse(document: Document): String = ""
}
