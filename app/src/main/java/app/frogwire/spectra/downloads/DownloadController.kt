package app.frogwire.spectra.downloads

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** A download the page asked for, waiting for the user to confirm. */
data class DownloadOffer(
    val url: String,
    val userAgent: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long,
    val host: String,
    val isIncognito: Boolean
)

enum class DownloadStatus { Running, Done, Failed }

data class DownloadItem(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val host: String,
    val isIncognito: Boolean,
    val createdAt: Long,
    /** Android DownloadManager id (standard tabs), or -1. */
    val systemId: Long = -1,
    /** MediaStore content URI (Incognito+), or null. */
    val contentUri: String? = null,
    val status: DownloadStatus = DownloadStatus.Running,
    val bytesDone: Long = 0,
    val bytesTotal: Long = -1
)

/**
 * Downloads, with two paths:
 * - Standard tabs hand off to Android's DownloadManager (progress notification, Downloads folder).
 * - Incognito+ downloads are fetched by Spectra itself and saved through MediaStore, so the
 *   system keeps no download history, and the incognito cookies never leave the app. Spectra's
 *   own record of them is in memory only and is dropped when the Incognito+ session is wiped.
 *
 * Every download asks first, only https is fetched, and files are never opened automatically.
 */
class DownloadController(
    private val appContext: Context,
    private val scope: CoroutineScope,
    /** Cookies for [url] from the Incognito+ profile. Called on the main thread. */
    private val incognitoCookies: (url: String) -> String?
) {
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)

    private val _offer = MutableStateFlow<DownloadOffer?>(null)
    val offer: StateFlow<DownloadOffer?> = _offer.asStateFlow()

    private val _items = MutableStateFlow(loadSaved())
    /** Newest first. */
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // region Asking

    /** WebView DownloadListener entry point. */
    fun onDownloadRequested(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        isIncognito: Boolean
    ) {
        val uri = Uri.parse(url)
        val secureUrl = when (uri.scheme?.lowercase()) {
            "https" -> url
            // HTTPS-only browser: never fetch a file in cleartext
            "http" -> uri.buildUpon().scheme("https").build().toString()
            "data" -> {
                offerDataUrl(url, isIncognito)
                return
            }
            else -> {
                // blob: downloads are generated inside the page; not supported yet
                toast("This kind of download isn't supported yet")
                return
            }
        }
        val type = mimeType?.substringBefore(';')?.trim()?.ifBlank { null } ?: "application/octet-stream"
        _offer.value = DownloadOffer(
            url = secureUrl,
            userAgent = userAgent.orEmpty(),
            fileName = safeFileName(URLUtil.guessFileName(url, contentDisposition, type)),
            mimeType = type,
            contentLength = contentLength,
            host = uri.host.orEmpty(),
            isIncognito = isIncognito
        )
    }

    /** A file embedded in the page itself (data: URL), e.g. an inline image. */
    private fun offerDataUrl(url: String, isIncognito: Boolean) {
        val header = url.substringAfter("data:", "").substringBefore(',')
        val type = header.substringBefore(';').trim().ifBlank { "application/octet-stream" }.lowercase()
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "bin"
        val base = if (type.startsWith("image/")) "image" else "download"
        val payloadLength = url.length - url.indexOf(',') - 1
        _offer.value = DownloadOffer(
            url = url,
            userAgent = "",
            fileName = "$base-${System.currentTimeMillis() / 1000}.$ext",
            mimeType = type,
            contentLength = if (header.endsWith(";base64")) payloadLength * 3L / 4 else payloadLength.toLong(),
            host = "",
            isIncognito = isIncognito
        )
    }

    fun dismissOffer() {
        _offer.value = null
    }

    fun acceptOffer() {
        val offer = _offer.value ?: return
        _offer.value = null
        when {
            offer.url.startsWith("data:") -> saveDataUrl(offer)
            offer.isIncognito -> startIncognito(offer)
            else -> startStandard(offer)
        }
    }

    // endregion

    // region Standard tabs: Android DownloadManager

    private fun startStandard(offer: DownloadOffer) {
        try {
            val request = DownloadManager.Request(Uri.parse(offer.url)).apply {
                setMimeType(offer.mimeType)
                setTitle(offer.fileName)
                if (offer.userAgent.isNotEmpty()) addRequestHeader("User-Agent", offer.userAgent)
                // Same login as the page (e.g. files behind a sign-in). No Referer is sent.
                CookieManager.getInstance().getCookie(offer.url)?.let { addRequestHeader("Cookie", it) }
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, offer.fileName)
                } else {
                    // Android 8-9 would need a storage permission for the public folder
                    setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, offer.fileName)
                }
            }
            val systemId = downloadManager.enqueue(request)
            add(newItem(offer).copy(systemId = systemId, bytesTotal = offer.contentLength))
            toast("Downloading ${offer.fileName}")
        } catch (e: Exception) {
            toast("Couldn't start the download")
        }
    }

    /** Updates progress of running standard downloads. Call while the Downloads list is visible. */
    fun refreshStatuses() {
        val running = _items.value.filter { it.systemId >= 0 && it.status == DownloadStatus.Running }
        if (running.isEmpty()) return
        val found = HashMap<Long, Triple<DownloadStatus, Long, Long>>()
        try {
            downloadManager.query(DownloadManager.Query().setFilterById(*running.map { it.systemId }.toLongArray()))
                ?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                    val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    val doneCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    while (c.moveToNext()) {
                        val status = when (c.getInt(statusCol)) {
                            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Done
                            DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
                            else -> DownloadStatus.Running
                        }
                        found[c.getLong(idCol)] = Triple(status, c.getLong(doneCol), c.getLong(totalCol))
                    }
                }
        } catch (e: Exception) {
            return
        }
        _items.update { list ->
            list.map { item ->
                if (item !in running) return@map item
                // Gone from DownloadManager: cancelled or deleted elsewhere
                val (status, done, total) = found[item.systemId] ?: Triple(DownloadStatus.Failed, 0L, -1L)
                item.copy(status = status, bytesDone = done, bytesTotal = total)
            }
        }
        persist()
    }

    // endregion

    // region Incognito+: fetched by Spectra, saved via MediaStore, no system history

    private fun startIncognito(offer: DownloadOffer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toast("Incognito+ downloads need Android 10 or newer")
            return
        }
        val cookies = incognitoCookies(offer.url) // main thread: WebView profile APIs require it
        val item = newItem(offer).copy(bytesTotal = offer.contentLength)
        add(item)
        toast("Downloading ${offer.fileName}")

        scope.launch(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            var target: Uri? = null
            try {
                val connection = (URL(offer.url).openConnection() as HttpURLConnection).apply {
                    // HttpURLConnection never follows an https -> http redirect
                    instanceFollowRedirects = true
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    if (offer.userAgent.isNotEmpty()) setRequestProperty("User-Agent", offer.userAgent)
                    cookies?.let { setRequestProperty("Cookie", it) }
                }
                try {
                    if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
                    val total = connection.contentLengthLong.takeIf { it > 0 } ?: offer.contentLength

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, offer.fileName)
                        put(MediaStore.Downloads.MIME_TYPE, offer.mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("No storage")
                    updateItem(item.id) { it.copy(contentUri = target.toString(), bytesTotal = total) }

                    connection.inputStream.use { input ->
                        resolver.openOutputStream(target)!!.use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            var lastReport = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                done += n
                                if (done - lastReport > 256 * 1024) {
                                    lastReport = done
                                    val d = done
                                    updateItem(item.id) { it.copy(bytesDone = d) }
                                }
                            }
                            updateItem(item.id) { it.copy(bytesDone = done) }
                        }
                    }
                    resolver.update(target, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    updateItem(item.id) { it.copy(status = DownloadStatus.Done) }
                    withContext(Dispatchers.Main) { toast("Saved ${offer.fileName} to Downloads") }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                target?.let { runCatching { resolver.delete(it, null, null) } }
                updateItem(item.id) { it.copy(status = DownloadStatus.Failed, contentUri = null) }
                withContext(Dispatchers.Main) { toast("Download failed: ${offer.fileName}") }
            }
        }
    }

    /** Decodes a data: URL and saves it to Downloads via MediaStore (no network involved). */
    private fun saveDataUrl(offer: DownloadOffer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toast("Saving this kind of file needs Android 10 or newer")
            return
        }
        val item = newItem(offer)
        add(item)
        scope.launch(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            var target: Uri? = null
            try {
                val header = offer.url.substringAfter("data:").substringBefore(',')
                val body = offer.url.substringAfter(',')
                val bytes = if (header.endsWith(";base64")) {
                    android.util.Base64.decode(body, android.util.Base64.DEFAULT)
                } else {
                    Uri.decode(body).toByteArray()
                }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, offer.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, offer.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("No storage")
                target = uri
                resolver.openOutputStream(uri)!!.use { it.write(bytes) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                updateItem(item.id) {
                    it.copy(contentUri = uri.toString(), status = DownloadStatus.Done, bytesDone = bytes.size.toLong(), bytesTotal = bytes.size.toLong())
                }
                withContext(Dispatchers.Main) {
                    persist()
                    toast("Saved ${offer.fileName} to Downloads")
                }
            } catch (e: Exception) {
                target?.let { runCatching { resolver.delete(it, null, null) } }
                updateItem(item.id) { it.copy(status = DownloadStatus.Failed) }
                withContext(Dispatchers.Main) { toast("Couldn't save ${offer.fileName}") }
            }
        }
    }

    /** Incognito+ closed: forget its downloads. The saved files stay in Downloads. */
    fun forgetIncognito() {
        _items.update { list -> list.filterNot { it.isIncognito } }
    }

    // endregion

    // region Opening, deleting, clearing

    fun open(context: Context, item: DownloadItem) {
        val uri = contentUriFor(item) ?: run {
            toast("This file is no longer available")
            return
        }
        // Never opened automatically; only when the user taps Open
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, item.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("No app on this phone can open this file")
        } catch (e: SecurityException) {
            toast("This file can't be opened")
        }
    }

    /** Deletes the file and its entry. */
    fun delete(item: DownloadItem) {
        try {
            if (item.systemId >= 0) {
                downloadManager.remove(item.systemId) // also deletes the file
            } else {
                item.contentUri?.let { appContext.contentResolver.delete(Uri.parse(it), null, null) }
            }
        } catch (_: Exception) {
            // Already gone; still drop the entry
        }
        _items.update { list -> list.filterNot { it.id == item.id } }
        persist()
    }

    /** Forgets every entry (Spectra's list only); the downloaded files stay. */
    fun clearList() {
        _items.value = emptyList()
        persist()
    }

    private fun contentUriFor(item: DownloadItem): Uri? = try {
        if (item.systemId >= 0) downloadManager.getUriForDownloadedFile(item.systemId)
        else item.contentUri?.let(Uri::parse)
    } catch (_: Exception) {
        null
    }

    // endregion

    // region Records

    private fun newItem(offer: DownloadOffer) = DownloadItem(
        id = UUID.randomUUID().toString(),
        fileName = offer.fileName,
        mimeType = offer.mimeType,
        host = offer.host,
        isIncognito = offer.isIncognito,
        createdAt = System.currentTimeMillis()
    )

    private fun add(item: DownloadItem) {
        _items.update { listOf(item) + it }
        persist()
    }

    private fun updateItem(id: String, change: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    /** Only standard downloads are saved; Incognito+ entries live in memory. */
    private fun persist() {
        val array = JSONArray()
        _items.value.filterNot { it.isIncognito }.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.fileName)
                    .put("mime", item.mimeType)
                    .put("host", item.host)
                    .put("at", item.createdAt)
                    .put("sys", item.systemId)
                    .put("uri", item.contentUri ?: "")
                    .put("status", item.status.name)
                    .put("done", item.bytesDone)
                    .put("total", item.bytesTotal)
            )
        }
        prefs.edit { putString(KEY_ITEMS, array.toString()) }
    }

    private fun loadSaved(): List<DownloadItem> = try {
        val array = JSONArray(prefs.getString(KEY_ITEMS, "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            DownloadItem(
                id = o.getString("id"),
                fileName = o.getString("name"),
                mimeType = o.optString("mime", "application/octet-stream"),
                host = o.optString("host"),
                isIncognito = false,
                createdAt = o.optLong("at"),
                systemId = o.optLong("sys", -1),
                contentUri = o.optString("uri").ifEmpty { null },
                status = runCatching { DownloadStatus.valueOf(o.optString("status")) }.getOrDefault(DownloadStatus.Done),
                bytesDone = o.optLong("done"),
                bytesTotal = o.optLong("total", -1)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    // endregion

    private fun toast(text: String) = Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()

    companion object {
        private const val PREFS_NAME = "spectra_downloads"
        private const val KEY_ITEMS = "items"

        /**
         * Makes a site-supplied file name safe to save: no folders, control characters,
         * hidden-file dots or reserved characters, and a sane length.
         */
        fun safeFileName(raw: String): String {
            var name = raw.substringAfterLast('/').substringAfterLast('\\')
                .filter { it >= ' ' && it !in "<>:\"|?*" }
                .trim()
                .trimStart('.')
            if (name.length > 120) {
                val ext = name.substringAfterLast('.', "").take(10)
                name = name.take(120 - ext.length - 1).trimEnd('.') + if (ext.isNotEmpty()) ".$ext" else ""
            }
            return name.ifBlank { "download" }
        }
    }
}
