package com.antoniegil.astronia.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object UpdateUtil {

    private const val OWNER = "antoniegil"
    private const val REPO = "Astronia"

    private val client = OkHttpClient()
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    private fun getLatestRelease(includePreRelease: Boolean): LatestRelease? {
        val url = if (includePreRelease) {
            "https://api.github.com/repos/${OWNER}/${REPO}/releases"
        } else {
            "https://api.github.com/repos/${OWNER}/${REPO}/releases/latest"
        }
        
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().run {
            val result = if (includePreRelease) {
                val releases = jsonFormat.decodeFromString<List<LatestRelease>>(body.string())
                releases.firstOrNull { it.preRelease }
            } else {
                jsonFormat.decodeFromString<LatestRelease>(body.string())
            }
            body.close()
            result
        }
    }

    fun checkForUpdate(context: Context, includePreRelease: Boolean = false): LatestRelease? {
        val currentVersion = context.getCurrentVersion()
        val latestRelease = getLatestRelease(includePreRelease) ?: return null
        val latestVersion = latestRelease.name.toVersion()
        return if (currentVersion < latestVersion) latestRelease
        else null
    }

    private fun Context.getCurrentVersion(): Version =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName, PackageManager.PackageInfoFlags.of(0)
            ).versionName.toVersion()
        } else {
            packageManager.getPackageInfo(
                packageName, 0
            ).versionName.toVersion()
        }

    @Serializable
    data class LatestRelease(
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val draft: Boolean? = null,
        @SerialName("prerelease") val preRelease: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("published_at") val publishedAt: String? = null,
        val assets: List<AssetsItem>? = null,
        val body: String? = null,
    )

    @Serializable
    data class AssetsItem(
        val name: String? = null,
        @SerialName("content_type") val contentType: String? = null,
        val size: Int? = null,
        @SerialName("download_count") val downloadCount: Int? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    )

    private val pattern = Pattern.compile("""v?(\d+)\.(\d+)\.(\d+)(-alpha\.(\d+))?""")
    private val EMPTY_VERSION = Version.Release()

    fun String?.toVersion(): Version = this?.run {
        val matcher = pattern.matcher(this)
        if (matcher.find()) {
            val major = matcher.group(1)?.toInt() ?: 0
            val minor = matcher.group(2)?.toInt() ?: 0
            val patch = matcher.group(3)?.toInt() ?: 0
            val buildNumber = matcher.group(5)?.toInt() ?: 0
            if (buildNumber > 0) {
                Version.PreRelease(major, minor, patch, buildNumber)
            } else {
                Version.Release(major, minor, patch)
            }
        } else EMPTY_VERSION
    } ?: EMPTY_VERSION

    sealed class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val build: Int = 0
    ) : Comparable<Version> {
        companion object {
            private const val BUILD = 1L
            private const val PATCH = 100L
            private const val MINOR = 10_000L
            private const val MAJOR = 1_000_000L
        }

        abstract fun toVersionName(): String
        abstract fun toNumber(): Long

        class PreRelease(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
            Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String =
                "${major}.${minor}.${patch}-alpha.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD
        }

        class Release(versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0) :
            Version(versionMajor, versionMinor, versionPatch) {
            override fun toVersionName(): String =
                "${major}.${minor}.${patch}"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + 100
        }

        override operator fun compareTo(other: Version): Int =
            this.toNumber().compareTo(other.toNumber())
    }
}
