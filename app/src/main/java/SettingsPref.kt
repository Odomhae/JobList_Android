import android.content.Context
import android.content.SharedPreferences

class SettingsPref(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settingsPreferences", Context.MODE_PRIVATE)

    fun saveTextScale(value: Float) {
        prefs.edit().putFloat("textScale", value).apply()
    }

    fun getTextScale(): Float {
        return prefs.getFloat("textScale", 1.0f)
    }

    fun saveSelectedRegions(codes: Set<String>) {
        prefs.edit().putStringSet("selectedRegions", codes).apply()
    }

    fun getSelectedRegions(): Set<String> {
        return prefs.getStringSet("selectedRegions", emptySet()) ?: emptySet()
    }
}
