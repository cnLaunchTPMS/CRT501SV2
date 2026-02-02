import android.os.Environment
import java.io.File

object SdCardFileUtil {

    private const val DIR_NAME = "Et3550"
    private const val FILE_NAME = "car_tree.txt"

    fun save(content: String): File {
        val root = Environment.getExternalStorageDirectory()  // 关键点

        val dir = File(root, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, FILE_NAME)
        file.writeText(content, Charsets.UTF_8)

        return file
    }

    fun read(): String {
        val root = Environment.getExternalStorageDirectory()
        val file = File(File(root, DIR_NAME), FILE_NAME)
        return file.readText(Charsets.UTF_8)
    }
}
