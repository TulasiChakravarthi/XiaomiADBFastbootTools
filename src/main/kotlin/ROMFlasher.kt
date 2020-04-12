import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextInputControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object ROMFlasher {

    var directory = XiaomiADBFastbootTools.dir
    lateinit var progressBar: ProgressBar
    lateinit var outputTextArea: TextInputControl
    lateinit var progressIndicator: ProgressIndicator

    private suspend fun setupScript(arg: String) = withContext(Dispatchers.IO) {
        if (XiaomiADBFastbootTools.win)
            File(directory, "script.bat").apply {
                try {
                    writeText(File(directory, "$arg.bat").readText().replace("fastboot", "${Command.prefix}fastboot"))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    ex.alert()
                }
                setExecutable(true, false)
            } else
            File(directory, "script.sh").apply {
                try {
                    writeText(File(directory, "$arg.sh").readText().replace("fastboot", "${Command.prefix}fastboot"))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    ex.alert()
                }
                setExecutable(true, false)
            }
    }

    suspend fun flash(arg: String?) {
        if (arg == null)
            return
        val sb = StringBuilder()
        withContext(Dispatchers.Main) {
            progressBar.progress = 0.0
            progressIndicator.isVisible = true
        }
        withContext(Dispatchers.IO) {
            val script = setupScript(arg)
            val command = if (XiaomiADBFastbootTools.win)
                mutableListOf("cmd.exe", "/c", script.absolutePath)
            else mutableListOf("sh", "-c", script.absolutePath)
            Scanner(startProcess(command, redirectErrorStream = true).inputStream, "UTF-8").useDelimiter("")
                .use { scanner ->
                    val n = script.readText().split("fastboot").size - 1
                    withContext(Dispatchers.Main) {
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine() + '\n')
                            val full = sb.toString()
                            if ("pause" in full)
                                break
                            outputTextArea.text = full
                            outputTextArea.appendText("")
                            progressBar.progress = 1.0 * (full.toLowerCase().split("finished.").size - 1) / n
                        }
                    }
                }
            script.delete()
        }
        withContext(Dispatchers.Main) {
            outputTextArea.appendText("\nDone!")
            progressBar.progress = 0.0
            progressIndicator.isVisible = false
        }
    }
}
