package io.github.csie1b.threadspulse.downloader

import io.github.csie1b.threadspulse.data.UrlCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class MediaItem(
    val url: String,
    val isVideo: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Double = 0.0,
    val mimeType: String = if (isVideo) "video/mp4" else "image/jpeg"
)

data class ThreadPost(
    val id: String,
    val author: String,
    val caption: String,
    val mediaItems: List<MediaItem>
)

object ThreadsMediaExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val SJS_PATTERN = Pattern.compile("<script type=\"application/json\" data-sjs>(.*?)</script>")

    suspend fun extract(rawUrl: String): Result<ThreadPost> = withContext(Dispatchers.IO) {
        val cleanedUrl = UrlCleaner.cleanUrl(rawUrl.trim())
        if (!cleanedUrl.contains("threads.net") && !cleanedUrl.contains("threads.com")) {
            return@withContext Result.failure(IllegalArgumentException("Not a valid Threads URL: $cleanedUrl"))
        }

        try {
            // First attempt: Request with modern browser user agent
            val request = Request.Builder()
                .url(cleanedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,zh-TW;q=0.8")
                .header("Sec-Fetch-Mode", "navigate")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val html = response.body?.string() ?: ""
            val post = parseFromHtml(html, cleanedUrl)
            if (post != null && post.mediaItems.isNotEmpty()) {
                return@withContext Result.success(post)
            }

            // Fallback: Parse OpenGraph meta tags
            val ogPost = parseOpenGraph(html, cleanedUrl)
            if (ogPost != null && ogPost.mediaItems.isNotEmpty()) {
                return@withContext Result.success(ogPost)
            }

            return@withContext Result.failure(IllegalStateException("No media found in the provided Threads post."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFromHtml(html: String, url: String): ThreadPost? {
        val matcher = SJS_PATTERN.matcher(html)
        val mediaList = mutableListOf<MediaItem>()
        var author = ""
        var caption = ""
        var postId = ""

        while (matcher.find()) {
            val jsonContent = matcher.group(1) ?: continue
            try {
                val root = JSONObject(jsonContent)
                findMediaInJson(root, mediaList)
            } catch (_: Exception) {}
        }

        if (mediaList.isNotEmpty()) {
            return ThreadPost(
                id = postId.ifEmpty { "threads_${System.currentTimeMillis()}" },
                author = author.ifEmpty { "Threads User" },
                caption = caption,
                mediaItems = mediaList.distinctBy { it.url }
            )
        }
        return null
    }

    private fun findMediaInJson(obj: Any?, out: MutableList<MediaItem>) {
        when (obj) {
            is JSONObject -> {
                // Check if this object represents a video
                if (obj.has("video_versions")) {
                    val versions = obj.optJSONArray("video_versions")
                    if (versions != null && versions.length() > 0) {
                        var bestUrl = ""
                        var maxW = 0
                        var maxH = 0
                        for (i in 0 until versions.length()) {
                            val v = versions.getJSONObject(i)
                            val w = v.optInt("width", 0)
                            val h = v.optInt("height", 0)
                            val url = v.optString("url")
                            if (w >= maxW && url.isNotEmpty()) {
                                maxW = w
                                maxH = h
                                bestUrl = url
                            }
                        }
                        if (bestUrl.isNotEmpty()) {
                            out.add(MediaItem(url = bestUrl, isVideo = true, width = maxW, height = maxH))
                        }
                    }
                }

                // Check image versions
                if (obj.has("image_versions2")) {
                    val imgVersions = obj.optJSONObject("image_versions2")
                    val candidates = imgVersions?.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        var bestUrl = ""
                        var maxW = 0
                        var maxH = 0
                        for (i in 0 until candidates.length()) {
                            val c = candidates.getJSONObject(i)
                            val w = c.optInt("width", 0)
                            val h = c.optInt("height", 0)
                            val url = c.optString("url")
                            if (w >= maxW && url.isNotEmpty()) {
                                maxW = w
                                maxH = h
                                bestUrl = url
                            }
                        }
                        if (bestUrl.isNotEmpty()) {
                            out.add(MediaItem(url = bestUrl, isVideo = false, width = maxW, height = maxH))
                        }
                    }
                }

                val keys = obj.keys()
                while (keys.hasNext()) {
                    findMediaInJson(obj.opt(keys.next()), out)
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    findMediaInJson(obj.opt(i), out)
                }
            }
        }
    }

    private fun parseOpenGraph(html: String, url: String): ThreadPost? {
        val mediaList = mutableListOf<MediaItem>()
        var author = "Threads User"
        var caption = ""

        // Extract og:video
        val videoPattern = Pattern.compile("<meta property=\"og:video\" content=\"(.*?)\"")
        val videoMatcher = videoPattern.matcher(html)
        while (videoMatcher.find()) {
            val videoUrl = videoMatcher.group(1)?.replace("&amp;", "&") ?: continue
            mediaList.add(MediaItem(url = videoUrl, isVideo = true))
        }

        // Extract og:image
        val imagePattern = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"")
        val imageMatcher = imagePattern.matcher(html)
        while (imageMatcher.find()) {
            val imageUrl = imageMatcher.group(1)?.replace("&amp;", "&") ?: continue
            mediaList.add(MediaItem(url = imageUrl, isVideo = false))
        }

        // Extract og:description
        val descPattern = Pattern.compile("<meta property=\"og:description\" content=\"(.*?)\"")
        val descMatcher = descPattern.matcher(html)
        if (descMatcher.find()) {
            caption = descMatcher.group(1)?.replace("&amp;", "&") ?: ""
        }

        // Extract og:title
        val titlePattern = Pattern.compile("<meta property=\"og:title\" content=\"(.*?)\"")
        val titleMatcher = titlePattern.matcher(html)
        if (titleMatcher.find()) {
            author = titleMatcher.group(1)?.replace("&amp;", "&") ?: "Threads User"
        }

        if (mediaList.isNotEmpty()) {
            return ThreadPost(
                id = "og_${System.currentTimeMillis()}",
                author = author,
                caption = caption,
                mediaItems = mediaList.distinctBy { it.url }
            )
        }
        return null
    }
}
