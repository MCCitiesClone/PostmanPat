package me.zodd.postmanpat.addons

import com.earth2me.essentials.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import litebans.api.Database
import me.zodd.postmanpat.PostmanPat

class LitebansAddon {
    private val litebansDB: Database
        get() {
            return Database.get()
        }

    private val deportationPhrase = PostmanPat.plugin.configManager.conf.moduleConfig.punishment.warnPhrase

    fun isLBMuted(user: User) = litebansDB.isPlayerMuted(user.uuid, user.name)

    fun isLBBanned(user: User) = litebansDB.isPlayerBanned(user.uuid, user.name)

    // This is primarily a "DC" specific feature in line with the deportations feature
    fun isDeported(user: User): Boolean {
        return runBlocking {
            CoroutineScope(Dispatchers.Default).async {
                val statement = Database.get().prepareStatement("SELECT * FROM {warnings} WHERE uuid=? AND active=1")
                statement.setString(1, user.uuid.toString())

                val result = statement.executeQuery()

                while (result.next()) {
                    result.getString("reason")?.let {
                        if (it.startsWith(deportationPhrase)) {
                            statement.close()
                            return@async true
                        }
                    }
                }
                statement.close()
                return@async false
            }.await()
        }
    }
}