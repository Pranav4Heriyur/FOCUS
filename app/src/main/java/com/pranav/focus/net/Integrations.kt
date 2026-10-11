package com.pranav.focus.net

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pranav.focus.data.Activity
import com.pranav.focus.data.Store
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.Earning
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

object Http {
    fun request(method: String, url: String, headers: Map<String, String> = emptyMap(), body: String? = null): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        return code to txt
    }

    fun form(vararg p: Pair<String, String>) = p.joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}" }
}

object NetState {
    var strava by mutableStateOf("")
    var notion by mutableStateOf("")
    var busy by mutableStateOf(false)
}

object Strava {
    const val REDIRECT = "focus://focus.app"

    fun clientId(ctx: Context) = Secure.get(ctx, "strava_id")
    fun hasKeys(ctx: Context) = clientId(ctx).isNotEmpty() && Secure.get(ctx, "strava_secret").isNotEmpty()
    fun connected(ctx: Context) = Secure.get(ctx, "strava_refresh").isNotEmpty()
    fun athlete(ctx: Context) = Secure.get(ctx, "strava_athlete")

    fun saveKeys(ctx: Context, id: String, secret: String) {
        Secure.put(ctx, "strava_id", id.trim()); Secure.put(ctx, "strava_secret", secret.trim())
    }

    fun authUri(ctx: Context): Uri = Uri.parse("https://www.strava.com/oauth/authorize").buildUpon()
        .appendQueryParameter("client_id", clientId(ctx))
        .appendQueryParameter("redirect_uri", REDIRECT)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("approval_prompt", "auto")
        .appendQueryParameter("scope", "read,activity:read_all")
        .build()

    fun disconnect(ctx: Context) {
        listOf("strava_access", "strava_refresh", "strava_exp", "strava_athlete").forEach { Secure.remove(ctx, it) }
        NetState.strava = ""
    }

    /** Handles focus://focus.app?code=... Returns true if it was ours. */
    fun handleRedirect(ctx: Context, uri: Uri?): Boolean {
        if (uri == null || uri.scheme != "focus" || uri.host != "focus.app") return false
        val code = uri.getQueryParameter("code")
        if (code == null) { NetState.strava = "Strava authorisation was cancelled."; return true }
        NetState.strava = "Connecting…"
        Thread {
            runCatching {
                val (c, t) = Http.request("POST", "https://www.strava.com/oauth/token",
                    mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    Http.form("client_id" to clientId(ctx), "client_secret" to Secure.get(ctx, "strava_secret"), "code" to code, "grant_type" to "authorization_code"))
                if (c !in 200..299) { NetState.strava = "Strava said no ($c). Check the Client ID and Secret."; return@runCatching }
                storeTokens(ctx, JSONObject(t))
                val a = JSONObject(t).optJSONObject("athlete")
                Secure.put(ctx, "strava_athlete", listOfNotNull(a?.optString("firstname"), a?.optString("lastname")).joinToString(" ").trim())
                sync(ctx)
            }.onFailure { NetState.strava = "Network error. Try again." }
        }.start()
        return true
    }

    private fun storeTokens(ctx: Context, o: JSONObject) {
        Secure.put(ctx, "strava_access", o.getString("access_token"))
        Secure.put(ctx, "strava_refresh", o.getString("refresh_token"))
        Secure.put(ctx, "strava_exp", o.getLong("expires_at").toString())
    }

    private fun token(ctx: Context): String? {
        val exp = Secure.get(ctx, "strava_exp").toLongOrNull() ?: 0
        if (exp > System.currentTimeMillis() / 1000 + 60) return Secure.get(ctx, "strava_access").ifEmpty { null }
        val refresh = Secure.get(ctx, "strava_refresh").ifEmpty { return null }
        val (c, t) = Http.request("POST", "https://www.strava.com/oauth/token",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            Http.form("client_id" to clientId(ctx), "client_secret" to Secure.get(ctx, "strava_secret"), "grant_type" to "refresh_token", "refresh_token" to refresh))
        if (c !in 200..299) return null
        val o = JSONObject(t)
        storeTokens(ctx, o)
        return o.getString("access_token")
    }

    /** Blocking; call off the main thread. */
    fun sync(ctx: Context) {
        runCatching {
            NetState.strava = "Syncing…"
            val tk = token(ctx) ?: run { NetState.strava = "Session expired. Reconnect Strava."; return }
            val after = System.currentTimeMillis() / 1000 - 45L * 86400
            val (c, t) = Http.request("GET", "https://www.strava.com/api/v3/athlete/activities?per_page=80&after=$after",
                mapOf("Authorization" to "Bearer $tk"))
            if (c !in 200..299) { NetState.strava = "Sync failed ($c)."; return }
            val arr = JSONArray(t)
            val out = ArrayList<Activity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val local = o.optString("start_date_local", o.optString("start_date"))
                out.add(Activity(
                    o.getLong("id"), o.optString("sport_type", o.optString("type")), o.optString("name"),
                    runCatching { Instant.parse(o.getString("start_date")).toEpochMilli() }.getOrDefault(0L), local.take(10),
                    o.optDouble("distance", 0.0), o.optInt("moving_time"), o.optDouble("average_heartrate", 0.0),
                    o.optDouble("total_elevation_gain", 0.0), o.optJSONObject("map")?.optString("summary_polyline").orEmpty(),
                ))
            }
            Store.get(ctx).activities = out.sortedByDescending { it.startMs }
            Earning.evaluate(ctx)
            Store.get(ctx).putLong("strava_last_sync", System.currentTimeMillis())
            NetState.strava = "Synced ${out.size} activities."
            AppState.dataTick++
        }.onFailure { NetState.strava = "Network error. Try again." }
    }

    fun syncAsync(ctx: Context) { Thread { sync(ctx) }.start() }
}

object Notion {
    fun configured(ctx: Context) = Secure.get(ctx, "notion_token").isNotEmpty() && Secure.get(ctx, "notion_db").isNotEmpty()

    fun cleanDb(raw: String): String {
        val hex = raw.replace("-", "")
        return Regex("[0-9a-fA-F]{32}").findAll(hex).lastOrNull()?.value ?: raw.trim()
    }

    fun save(ctx: Context, token: String, db: String) {
        Secure.put(ctx, "notion_token", token.trim()); Secure.put(ctx, "notion_db", cleanDb(db))
    }

    fun disconnect(ctx: Context) { Secure.remove(ctx, "notion_token"); Secure.remove(ctx, "notion_db"); NetState.notion = "" }

    private fun headers(ctx: Context) = mapOf(
        "Authorization" to "Bearer ${Secure.get(ctx, "notion_token")}",
        "Notion-Version" to "2022-06-28",
        "Content-Type" to "application/json",
    )

    private fun titleProp(ctx: Context): String? {
        val (c, t) = Http.request("GET", "https://api.notion.com/v1/databases/${Secure.get(ctx, "notion_db")}", headers(ctx))
        if (c !in 200..299) return null
        val props = JSONObject(t).optJSONObject("properties") ?: return null
        for (k in props.keys()) if (props.getJSONObject(k).optString("type") == "title") return k
        return null
    }

    /** Blocking. Returns a human-readable result. */
    fun push(ctx: Context, title: String, body: String): String {
        if (!configured(ctx)) return "Notion isn’t set up."
        return runCatching {
            val prop = titleProp(ctx) ?: return "Couldn’t read the database. Share it with your integration."
            val page = JSONObject()
                .put("parent", JSONObject().put("database_id", Secure.get(ctx, "notion_db")))
                .put("properties", JSONObject().put(prop, JSONObject().put("title", JSONArray().put(JSONObject().put("text", JSONObject().put("content", title))))))
                .put("children", JSONArray().put(JSONObject().put("object", "block").put("type", "paragraph")
                    .put("paragraph", JSONObject().put("rich_text", JSONArray().put(JSONObject().put("type", "text").put("text", JSONObject().put("content", body)))))))
            val (c, _) = Http.request("POST", "https://api.notion.com/v1/pages", headers(ctx), page.toString())
            if (c in 200..299) "Sent to Notion." else "Notion said no ($c)."
        }.getOrDefault("Network error. Try again.")
    }

    fun pushAsync(ctx: Context, title: String, body: String, report: Boolean = true) {
        Thread { val r = push(ctx, title, body); if (report) NetState.notion = r }.start()
    }
}
