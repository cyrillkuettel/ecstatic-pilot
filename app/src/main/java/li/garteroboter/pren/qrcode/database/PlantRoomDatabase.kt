package li.garteroboter.pren.qrcode.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = arrayOf(Plant::class), version = 1, exportSchema = false)
abstract class PlantRoomDatabase : RoomDatabase() {

    abstract fun plantDataAccessObject(): PlantDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: PlantRoomDatabase? = null

        fun getDatabase(context: Context): PlantRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlantRoomDatabase::class.java,
                    "plant_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
