package com.example.goodstart.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.goodstart.ui.screen.*

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onStudyClick = { key, date, title, label ->
                    nav.navigate("study/$key/$date/${Uri.encode(title)}/${Uri.encode(label)}")
                },
                onZmanimClick = { nav.navigate("zmanim") },
                onSettingsClick = { nav.navigate("settings") },
                onLocationZonesClick = { nav.navigate("locationZones") },
                onRabbenuTamClick = { nav.navigate("rabbenuTam") },
                onPdfLibraryClick = { nav.navigate("pdfLibrary") },
                onMamaarimClick = { nav.navigate("mamaarim") },
                onStudyTrackerClick = { nav.navigate("studyTracker") }
            )
        }

        composable(
            "study/{key}/{date}/{title}/{label}",
            arguments = listOf(
                navArgument("key")   { type = NavType.StringType },
                navArgument("date")  { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("label") { type = NavType.StringType }
            )
        ) { back ->
            StudyReadingScreen(
                studyKey = back.arguments?.getString("key") ?: "",
                date     = back.arguments?.getString("date") ?: "",
                title    = Uri.decode(back.arguments?.getString("title") ?: ""),
                label    = Uri.decode(back.arguments?.getString("label") ?: ""),
                onBack     = { nav.popBackStack() },
                onSettings = { nav.navigate("settings") }
            )
        }

        composable("zmanim") {
            ZmanimScreen(onBack = { nav.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }

        composable("locationZones") {
            LocationZoneScreen(onBack = { nav.popBackStack() })
        }

        composable("rabbenuTam") {
            RabbenuTamScreen(onBack = { nav.popBackStack() })
        }

        // ── הספרייה האישית — המסך הישן (PDF כתמונה) ────────────────────────
        composable("pdfLibrary") {
            PdfStudyScreen(onBack = { nav.popBackStack() })
        }

        // ── מאמרים — האזור החדש (חילוץ טקסט) ──────────────────────────────
        composable("mamaarim") {
            MamaarimScreen(
                onBack   = { nav.popBackStack() },
                onOpen   = { id -> nav.navigate("mamaarReader/$id") },
                onUpload = { nav.navigate("uploadArticle") }
            )
        }

        composable("uploadArticle") {
            ArticleUploadScreen(
                onBack    = { nav.popBackStack() },
                onSuccess = {
                    nav.popBackStack()
                }
            )
        }

        composable("mamaarReader/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { back ->
            MamaarReaderScreen(
                mamaarId = back.arguments?.getString("id") ?: "",
                onBack   = { nav.popBackStack() }
            )
        }

        composable("studyTracker") {
            StudyTrackerScreen(onBack = { nav.popBackStack() })
        }
    }
}
