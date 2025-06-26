package org.cssnr.tibs3dprints.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import org.cssnr.tibs3dprints.api.ServerApi.UserResponse

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'singleton' LIMIT 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(profile: UserProfile)

    @Query("UPDATE user_profile SET points = :points WHERE id = 'singleton'")
    suspend fun setPoints(points: Long)

    @Query("UPDATE user_profile SET lastLogin = :timestamp WHERE id = 'singleton'")
    suspend fun updateLastLoginMillis(timestamp: Long)
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "singleton",
    val authorization: String,
    val email: String,
    val name: String?,
    val avatar: String?,
    val points: Long,
    val lastLogin: Long,
)

@Database(entities = [UserProfile::class], version = 2, exportSchema = false)
abstract class UserProfileDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: UserProfileDatabase? = null

        fun getInstance(context: Context): UserProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    UserProfileDatabase::class.java,
                    "user_profile.db"
                )
                    .fallbackToDestructiveMigration(true) // Destructive Operation
                    .build().also { INSTANCE = it }
            }
        }
    }
}

interface UserProfileRepository {
    suspend fun get(): UserProfile?
    suspend fun put(profile: UserProfile)
    suspend fun putData(userData: UserResponse)
    suspend fun setPoints(points: Long)
    suspend fun updateLastLogin()

    companion object {
        fun getInstance(context: Context): UserProfileRepository {
            val dao = UserProfileDatabase.getInstance(context).userProfileDao()
            return object : UserProfileRepository {
                override suspend fun get(): UserProfile? = dao.get()
                override suspend fun put(profile: UserProfile) = dao.put(profile)
                override suspend fun putData(userData: UserResponse) {
                    dao.put(
                        UserProfile(
                            authorization = userData.authorization,
                            email = userData.email,
                            name = userData.name,
                            avatar = null,
                            points = userData.points.toLong(),
                            lastLogin = System.currentTimeMillis(),
                        )
                    )
                }

                override suspend fun setPoints(points: Long) = dao.setPoints(points)
                override suspend fun updateLastLogin() {
                    val now = System.currentTimeMillis()
                    dao.updateLastLoginMillis(now)
                }
            }
        }
    }
}
