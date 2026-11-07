package com.example.controldegastos.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controldegastos.R
import com.example.controldegastos.ui.screens.HomeScreen
import com.example.controldegastos.ui.screens.IncomeScreen
import com.example.controldegastos.ui.screens.ItemDetailScreen
import com.example.controldegastos.ui.screens.ItemFormScreen
import com.example.controldegastos.ui.screens.ItemsScreen
import com.example.controldegastos.ui.screens.LoginScreen
import com.example.controldegastos.ui.screens.ProjectionScreen
import com.example.controldegastos.ui.screens.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Pestañas de la barra inferior. */
enum class Tab(val label: String) { HOME("Inicio"), ITEMS("Deudas"), INCOME("Ingresos"), PROJECTION("Proyección") }

/** Pantallas que se abren encima de las pestañas. */
sealed interface Route {
    data class ItemDetail(val id: String) : Route
    /** [id] null = crear nuevo. */
    data class ItemForm(val id: String? = null) : Route
    data object Settings : Route

    fun key(): String = when (this) {
        is ItemDetail -> "detail:$id"
        is ItemForm -> "form:${id.orEmpty()}"
        Settings -> "settings"
    }

    companion object {
        fun fromKey(key: String): Route? = when {
            key.startsWith("detail:") -> ItemDetail(key.removePrefix("detail:"))
            key.startsWith("form:") -> ItemForm(key.removePrefix("form:").ifEmpty { null })
            key == "settings" -> Settings
            else -> null
        }
    }
}

/** Snackbar global (para mensajes con "Deshacer" que sobreviven al cerrar una pantalla). */
class AppSnackbar(val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = actionLabel == null,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbar> { error("AppSnackbar no disponible") }

/** Acciones de navegación disponibles para las pantallas. */
class Navigator(
    private val stack: MutableList<Route>,
    private val holder: SaveableStateHolder,
    private val selectTab: (Tab) -> Unit,
) {
    fun open(route: Route) {
        holder.removeState(route.key()) // una pantalla nueva siempre empieza limpia
        stack.add(route)
    }

    fun back() {
        if (stack.isNotEmpty()) holder.removeState(stack.removeAt(stack.lastIndex).key())
    }

    fun replace(route: Route) { back(); open(route) }

    fun goToTab(tab: Tab) {
        while (stack.isNotEmpty()) back()
        selectTab(tab)
    }
}

/** Abre el enlace de descarga de una actualización. */
fun openUpdate(context: Context, info: UpdateInfo) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, info.apkUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() }
}

@Composable
fun AppRoot(viewModel: FinanceViewModel) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    AnimatedContent(targetState = user == null, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "auth") { loggedOut ->
        if (loggedOut) LoginScreen() else MainShell(viewModel)
    }
}

@Composable
private fun MainShell(viewModel: FinanceViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val stack = rememberSaveable(
        saver = listSaver(save = { list -> list.map { it.key() } }, restore = { keys -> keys.mapNotNull(Route::fromKey).toMutableStateList() }),
    ) { mutableListOf<Route>().toMutableStateList() }
    val holder = rememberSaveableStateHolder()
    val navigator = remember(stack, holder) { Navigator(stack, holder) { tab = it } }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appSnackbar = remember { AppSnackbar(snackbarHost, scope) }

    BackHandler(enabled = stack.isNotEmpty() || tab != Tab.HOME) {
        if (stack.isNotEmpty()) navigator.back() else tab = Tab.HOME
    }

    CompositionLocalProvider(LocalAppSnackbar provides appSnackbar) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = stack.lastOrNull(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "route",
            ) { route ->
                holder.SaveableStateProvider(route?.key() ?: "tabs") {
                    when (route) {
                        null -> TabsScaffold(viewModel, tab, onTab = { tab = it }, navigator = navigator, holder = holder)
                        is Route.ItemDetail -> ItemDetailScreen(viewModel, route.id, navigator)
                        is Route.ItemForm -> ItemFormScreen(viewModel, route.id, navigator)
                        Route.Settings -> SettingsScreen(viewModel, navigator)
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (stack.isEmpty()) 80.dp else 8.dp),
            )
        }
    }

    UpdateChecker(viewModel)
}

@Composable
private fun TabsScaffold(
    viewModel: FinanceViewModel,
    tab: Tab,
    onTab: (Tab) -> Unit,
    navigator: Navigator,
    holder: SaveableStateHolder,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { onTab(t) },
                        icon = { Icon(tabIcon(t, t == tab), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            holder.SaveableStateProvider("tab:${tab.name}") {
                when (tab) {
                    Tab.HOME -> HomeScreen(viewModel, navigator)
                    Tab.ITEMS -> ItemsScreen(viewModel, navigator)
                    Tab.INCOME -> IncomeScreen(viewModel)
                    Tab.PROJECTION -> ProjectionScreen(viewModel, navigator)
                }
            }
        }
    }
}

@Composable
private fun tabIcon(tab: Tab, selected: Boolean): Painter = when (tab) {
    Tab.HOME -> rememberVectorPainter(if (selected) Icons.Filled.Home else Icons.Outlined.Home)
    Tab.ITEMS -> painterResource(R.drawable.ic_credit_card)
    Tab.INCOME -> painterResource(R.drawable.ic_money)
    Tab.PROJECTION -> painterResource(R.drawable.ic_trending_up)
}

/** Revisa una vez por sesión si hay una versión nueva publicada en Remote Config. */
@Composable
private fun UpdateChecker(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var checked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!checked) {
            checked = true
            viewModel.checkForUpdates { update = it }
        }
    }
    update?.let { info ->
        AlertDialog(
            onDismissRequest = { update = null },
            title = { Text("Nueva versión ${info.versionName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (info.notes.isNotBlank()) Text(info.notes)
                    Text("Descarga e instala la nueva versión para tener las últimas mejoras.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    openUpdate(context, info)
                    update = null
                }) { Text("Descargar") }
            },
            dismissButton = { TextButton(onClick = { update = null }) { Text("Más tarde") } },
        )
    }
}
