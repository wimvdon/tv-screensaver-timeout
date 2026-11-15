package to.wim.screensavertimeout.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import to.wim.screensavertimeout.ui.home.HomeScreen
import to.wim.screensavertimeout.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    /** Goes up every time the app comes to the front, so the screen reads the TV's values again. */
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                HomeScreen(resumes = resumes)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }
}
