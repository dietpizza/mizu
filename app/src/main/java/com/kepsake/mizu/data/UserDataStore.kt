import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define DataStore at the top level of your app
val Context.dataStore by preferencesDataStore(name = "user.data")

/**
 * Simple utility class to store and retrieve user data
 */
class UserDataStore(private val context: Context) {

    /**
     * Saves a string value with the given key
     * @param key The identifier for the data
     * @param value The string value to store
     */
    suspend fun saveString(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[dataStoreKey] = value
        }
    }

    /**
     * Retrieves a string value by key
     * @param key The identifier for the data
     * @param defaultValue The default value to return if the key doesn't exist
     * @return Flow of the string value
     */
    fun getString(key: String, defaultValue: String = ""): Flow<String> {
        val dataStoreKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }
    }

    /**
     * Gets a string value immediately (blocking call)
     * Use this only when you need an immediate value, not in composables
     */
    suspend fun getStringSync(key: String, defaultValue: String = ""): String {
        val dataStoreKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }.first()  // Use first() to get the first emission
    }

    /**
     * Clears all user data
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Removes a specific key
     */
    suspend fun remove(key: String) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences.remove(dataStoreKey)
        }
    }
}