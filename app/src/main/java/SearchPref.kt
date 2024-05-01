import android.content.Context
import android.content.SharedPreferences

class SearchPref(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("mySharedPreferences", Context.MODE_PRIVATE)
    fun saveEducation(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString("Education", value)
        editor.apply()
    }

    fun getEducation(): String {
        return sharedPreferences.getString("Education",  "%20")!!
    }

    fun saveStyle(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString("Style", value)
        editor.apply()
    }

    fun getStyle(): String {
        return sharedPreferences.getString("Style",  "%20")!!
    }


    fun saveLocation(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString("Location", value)
        editor.apply()
    }

    fun getLocation(): String {
        return sharedPreferences.getString("Location",  "%20")!!
    }

    fun saveCareer(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString("Career", value)
        editor.apply()
    }

    fun getCareer(): String {
        return sharedPreferences.getString("Career",  "%20")!!
    }


}