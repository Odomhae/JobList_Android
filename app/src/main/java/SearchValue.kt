import android.content.Context

class SearchValue {

    companion object {

        var education : String = "%20"
        var style : String =  "%20"
        var location : String = "%20"
        var career : String =  "%20"

        fun saveLocation(context: Context, value: String) {
            val sharedPreferences = context.getSharedPreferences("mySharedPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("Location", value)
            editor.apply()
        }

        fun getLocation(context: Context): String {
            val sharedPreferences = context.getSharedPreferences("mySharedPreferences", Context.MODE_PRIVATE)
            return sharedPreferences.getString("Location",  "%20")!!
        }
    }


}