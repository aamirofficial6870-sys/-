package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.GameViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07070F)
                ) {
                    val navController = rememberNavController()
                    val viewModel: GameViewModel = viewModel()

                    val profile by viewModel.userProfile.collectAsState()
                    val achievements by viewModel.achievements.collectAsState()
                    val leaderboard by viewModel.leaderboard.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "menu"
                    ) {
                        composable("menu") {
                            MainMenuScreen(
                                userProfile = profile,
                                onNavigateToGame = { navController.navigate("game") },
                                onNavigateToUpgrades = { navController.navigate("upgrades") },
                                onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                                onNavigateToDaily = { navController.navigate("daily") },
                                onNavigateToAchievements = { navController.navigate("achievements") },
                                onChangeUsername = { name -> viewModel.changeUsername(name) }
                            )
                        }

                        composable("game") {
                            GameScreen(
                                gameEngine = viewModel.gameEngine,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("upgrades") {
                            UpgradesScreen(
                                userProfile = profile,
                                onUpgrade = { type, onResult -> viewModel.upgrade(type, onResult) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("leaderboard") {
                            LeaderboardScreen(
                                leaderboard = leaderboard,
                                onForceSync = { viewModel.forceLeaderboardSync() },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("daily") {
                            DailyRewardsScreen(
                                userProfile = profile,
                                onClaimReward = { callback -> viewModel.claimDailyReward(callback) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("achievements") {
                            AchievementsScreen(
                                achievements = achievements,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
