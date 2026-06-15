package eu.kanade.tachiyomi.extension.ar.procomic

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ProComic : ParsedHttpSource() {
    override val name = "ProComic"
    override val baseUrl = "https://procomic.pro"
    override val lang = "ar"
    override val supportsLatest = true
    override val client = network.cloudflareClient

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/series?page=$page&sort=views", headers)
    override fun popularMangaSelector() = "div.series-card, .comic-card, article"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        title = element.selectFirst("h3, h2, .title")?.text() ?: ""
        setUrlWithoutDomain(element.selectFirst("a")?.attr("href") ?: "")
        thumbnail_url = element.selectFirst("img")?.attr("src")
    }
    override fun popularMangaNextPageSelector() = "a[rel=next], .next"

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/series?page=$page&sort=latest", headers)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page", page.toString())
        return GET(url.build(), headers)
    }
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.selectFirst("h1")?.text() ?: ""
        thumbnail_url = document.selectFirst("img.cover, .series-cover img")?.attr("src")
        author = document.selectFirst(".author, [itemprop=author]")?.text()
        description = document.selectFirst(".synopsis, .description")?.text()
        genre = document.select("a.genre-tag, .tags a").joinToString(", ") { it.text() }
        status = when (document.selectFirst(".status")?.text()?.trim()) {
            "مستمر" -> SManga.ONGOING
            "مكتمل" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    override fun chapterListSelector() = ".chapter-item, .chapters-list li"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.selectFirst("a")?.attr("href") ?: "")
        name = element.selectFirst("a, .chapter-title")?.text() ?: ""
        chapter_number = name.substringAfterLast(" ").toFloatOrNull() ?: -1f
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.reader-images img, .chapter-images img, img[src*='/uploads/']")
            .mapIndexed { i, img -> Page(i, "", img.attr("abs:src").ifBlank { img.attr("abs:data-src") }) }
    }

    override fun imageUrlParse(document: Document) = ""
}
