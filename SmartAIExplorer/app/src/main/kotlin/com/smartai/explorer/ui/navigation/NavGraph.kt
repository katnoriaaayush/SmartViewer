package com.smartai.explorer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartai.explorer.ui.screens.home.HomeScreen
import com.smartai.explorer.ui.screens.viewer.ViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder

private const val HOME   = "home"
private const val VIEWER = "viewer"

@Composable
fun SmartAINavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HOME) {

        composable(HOME) {
            HomeScreen(onOpenViewer = { fileUri, fileName, startPage, endPage ->
                val encodedUri  = URLEncoder.encode(fileUri, "UTF-8")
                val encodedName = URLEncoder.encode(fileName, "UTF-8")
                navController.navigate("$VIEWER?fileUri=$encodedUri&fileName=$encodedName&startPage=$startPage&endPage=$endPage")
            })
        }

        composable(
            route = "$VIEWER?fileUri={fileUri}&fileName={fileName}&startPage={startPage}&endPage={endPage}",
            arguments = listOf(
                navArgument("fileUri")   { type = NavType.StringType },
                navArgument("fileName")  { type = NavType.StringType },
                navArgument("startPage") { type = NavType.IntType; defaultValue = 1 },
                navArgument("endPage")   { type = NavType.IntType; defaultValue = 1 },
            ),
        ) { entry ->
            val args      = entry.arguments!!
            ViewerScreen(
                fileUri   = URLDecoder.decode(args.getString("fileUri")!!,  "UTF-8"),
                fileName  = URLDecoder.decode(args.getString("fileName")!!, "UTF-8"),
                startPage = args.getInt("startPage"),
                endPage   = args.getInt("endPage"),
                onBack    = { navController.popBackStack() },
            )
        }
    }
}
