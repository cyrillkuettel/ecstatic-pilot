package li.garteroboter.pren.qrcode.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "plant_table")
class Plant(
    @ColumnInfo(name = "qrString") val qrString: String, // The decoded Qr-Code String
    @ColumnInfo(name = "imageUri") val imageUri: String, // onQRCodeFound triggers a photo capture // saves the file. imageUri is the path to that file.
    @ColumnInfo(name = "species") val species: String

)  {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
