import androidx.compose.material3.*
import androidx.compose.ui.unit.Density

@OptIn(ExperimentalMaterial3Api::class)
fun check() {
    val state = SheetState(
        skipPartiallyExpanded = false,
        density = Density(1f)
    )
}
