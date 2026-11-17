package io.github.csie1b.threadspulse.data

import android.net.Uri
import java.util.regex.Pattern

object UrlCleaner {

    private val EXACT_TRACKING_PARAMS = setOf(
        "xmt", "slof", "igshid", "igsh", "ig_rid", "ig_mid", "ig_cache_key",
        "fbclid", "mibextid", "__tn__", "__cft__", "__xts__", "fb_action_ids",
        "fb_source", "ref_src", "ref_url",
        "gclid", "gclsrc", "dclid", "msclkid", "twclid", "yclid", "_hsenc", "_hsmi",
        "mc_cid", "mc_eid", "si"
    )

    private val REDIRECT_HOSTS = setOf(
        "l.threads.com", "l.threads.net", "l.instagram.com",
        "l.facebook.com", "lm.facebook.com"
    )

    private val URL_PATTERN = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")

    fun isTrackingParam(name: String): Boolean {
        val lower = name.lowercase()
        return EXACT_TRACKING_PARAMS.contains(lower) || lower.startsWith("utm_")
    }

    fun isRedirectHost(host: String?): Boolean {
        if (host == null) return false
        return REDIRECT_HOSTS.contains(host.lowercase())
    }

    fun cleanUrl(urlStr: String?): String {
        if (urlStr.isNullOrBlank()) return ""
        try {
            val uri = Uri.parse(urlStr)
            val scheme = uri.scheme?.lowercase() ?: return urlStr
            if (scheme != "http" && scheme != "https") return urlStr

            // Unwrap redirect URLs
            if (isRedirectHost(uri.host)) {
                val targetUrl = uri.getQueryParameter("u")
                if (!targetUrl.isNullOrEmpty()) {
                    return cleanUrl(targetUrl)
                }
            }

            val queryParamNames = uri.queryParameterNames
            if (queryParamNames.isEmpty()) return urlStr

            val builder = uri.buildUpon().clearQuery()
            var changed = false

            for (paramName in queryParamNames) {
                if (isTrackingParam(paramName)) {
                    changed = true
                    continue
                }
                for (value in uri.getQueryParameters(paramName)) {
                    builder.appendQueryParameter(paramName, value)
                }
            }

            return if (changed) builder.build().toString() else urlStr
        } catch (_: Exception) {
            return urlStr
        }
    }

    fun cleanString(text: String?): String {
        if (text.isNullOrBlank() || !text.contains("http", ignoreCase = true)) {
            return text ?: ""
        }

        val matcher = URL_PATTERN.matcher(text)
        val sb = StringBuffer()

        while (matcher.find()) {
            val originalUrl = matcher.group()
            val cleaned = cleanUrl(originalUrl)
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(cleaned))
        }
        matcher.appendTail(sb)

        return sb.toString()
    }
}
