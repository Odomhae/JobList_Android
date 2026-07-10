import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.odom.seoulJobInfo.JobInfo

class FavoritePref(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("favoritePreferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun add(job: JobInfo) {
        val id = job.joReqstNo ?: return
        val current = loadRawSet().toMutableSet()
        current.removeAll { gson.fromJson(it, JobInfo::class.java).joReqstNo == id }
        current.add(gson.toJson(job))
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun remove(joReqstNo: String) {
        val current = loadRawSet().toMutableSet()
        current.removeAll { gson.fromJson(it, JobInfo::class.java).joReqstNo == joReqstNo }
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun isFavorite(joReqstNo: String): Boolean =
        loadRawSet().any { gson.fromJson(it, JobInfo::class.java).joReqstNo == joReqstNo }

    fun getAll(): List<JobInfo> =
        loadRawSet().mapNotNull { runCatching { gson.fromJson(it, JobInfo::class.java) }.getOrNull() }

    fun incrementAndGetAddCount(): Int {
        val next = prefs.getInt("favoritesAddCount", 0) + 1
        prefs.edit().putInt("favoritesAddCount", next).apply()
        return next
    }

    private fun loadRawSet(): Set<String> =
        prefs.getStringSet("favorites", emptySet()) ?: emptySet()
}
