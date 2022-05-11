package app.revanced.manager.ui.screens

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph

@NavGraph
@RootNavGraph(start = true)
annotation class MainNavGraph(
    val start: Boolean = false
)