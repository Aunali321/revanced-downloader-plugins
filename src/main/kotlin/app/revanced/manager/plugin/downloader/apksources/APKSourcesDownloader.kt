@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.apksources

import android.net.Uri
import app.revanced.manager.plugin.downloader.webview.WebViewDownloader

val apkSourcesDownloader = WebViewDownloader { packageName, version ->
    val sources = listOf(
        SourceInfo("APKPure") { pkg, ver ->
            with(Uri.Builder()) {
                scheme("https")
                authority("apkpure.net")
                path("/android/$pkg")
                ver?.let { appendQueryParameter("version", it) }
                build().toString()
            }
        },
        SourceInfo("APKMirror") { pkg, ver ->
            with(Uri.Builder()) {
                scheme("https")
                authority("www.apkmirror.com")
                mapOf(
                    "post_type" to "app_release",
                    "searchtype" to "apk",
                    "s" to (ver?.let { "$pkg $it" } ?: pkg),
                    "bundles%5B%5D" to "apk_files"
                ).forEach { (key, value) ->
                    appendQueryParameter(key, value)
                }
                build().toString()
            }
        },
        SourceInfo("APKCombo") { pkg, ver ->
            with(Uri.Builder()) {
                scheme("https")
                authority("apkcombo.com")
                path("/en/app/$pkg")
                ver?.let { appendQueryParameter("version", it) }
                build().toString()
            }
        }
    )
    
    var currentSourceIndex = 0
    
    pageLoad { url ->
        // Detect common failure patterns
        val isErrorPage = url.contains("challenge") || 
                         url.contains("cloudflare") ||
                         url.contains("security-check")
        
        if (isErrorPage && currentSourceIndex + 1 < sources.size) {
            currentSourceIndex++
            val nextSource = sources[currentSourceIndex]
            
            // Navigate to next source
            load(nextSource.urlProvider(packageName, version))
        }
    }
    
    // Start with the first source
    sources[currentSourceIndex].urlProvider(packageName, version)
}

private data class SourceInfo(
    val name: String,
    val urlProvider: (String, String?) -> String
) 