package com.virudaya.cavy.ui.navigation

import java.net.URLDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.virudaya.cavy.ui.auth.AuthSessionState
import com.virudaya.cavy.ui.auth.AuthSplashScreen
import com.virudaya.cavy.ui.auth.AuthViewModel
import com.virudaya.cavy.ui.auth.ForgotPasswordScreen
import com.virudaya.cavy.ui.auth.RegisterScreen
import com.virudaya.cavy.ui.auth.SignInScreen
import com.virudaya.cavy.ui.auth.WelcomeScreen
import com.virudaya.cavy.ui.home.CavyHomeScreen
import com.virudaya.cavy.ui.messaging.ChatScreen
import com.virudaya.cavy.ui.messaging.ChatViewModel
import com.virudaya.cavy.ui.messaging.ConversationListScreen
import com.virudaya.cavy.ui.messaging.ConversationListViewModel
import com.virudaya.cavy.ui.profile.ProfileViewModel

@Composable
fun CavyNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = viewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    when (val state = sessionState) {
        is AuthSessionState.Loading -> {
            AuthSplashScreen(modifier = modifier)
        }

        else -> {
            val startDestination = if (state is AuthSessionState.Authenticated) {
                Screen.Home.route
            } else {
                Screen.Welcome.route
            }

            LaunchedEffect(sessionState) {
                when (sessionState) {
                    is AuthSessionState.Authenticated -> {
                        if (navController.currentDestination?.route == Screen.Welcome.route ||
                            navController.currentDestination?.route == Screen.SignIn.route ||
                            navController.currentDestination?.route == Screen.Register.route
                        ) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthSessionState.Unauthenticated -> {
                        if (navController.currentDestination?.route != Screen.Welcome.route) {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    else -> {}
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onNavigateToSignIn = {
                            viewModel.clearMessages()
                            navController.navigate(Screen.SignIn.route)
                        },
                        onNavigateToRegister = {
                            viewModel.clearMessages()
                            navController.navigate(Screen.Register.route)
                        }
                    )
                }

                composable(Screen.SignIn.route) {
                    SignInScreen(
                        uiState = uiState,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onSignInClicked = {
                            viewModel.signIn()
                        },
                        onNavigateBack = {
                            viewModel.clearMessages()
                            navController.popBackStack()
                        },
                        onNavigateToRegister = {
                            viewModel.clearMessages()
                            navController.navigate(Screen.Register.route) {
                                popUpTo(Screen.Welcome.route)
                            }
                        },
                        onNavigateToForgotPassword = {
                            viewModel.clearMessages()
                            navController.navigate(Screen.ForgotPassword.route)
                        }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        uiState = uiState,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                        onRegisterClicked = {
                            viewModel.register()
                        },
                        onNavigateBack = {
                            viewModel.clearMessages()
                            navController.popBackStack()
                        },
                        onNavigateToSignIn = {
                            viewModel.clearMessages()
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(Screen.Welcome.route)
                            }
                        }
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        uiState = uiState,
                        onEmailChanged = viewModel::onEmailChanged,
                        onSendResetClicked = {
                            viewModel.sendPasswordReset()
                        },
                        onNavigateBack = {
                            viewModel.clearMessages()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val profileViewModel: ProfileViewModel = viewModel()
                    val profileUiState by profileViewModel.uiState.collectAsState()

                    CavyHomeScreen(
                        profileUiState = profileUiState,
                        onStartEditDisplayName = profileViewModel::startEditingDisplayName,
                        onDisplayNameChange = profileViewModel::onDisplayNameInputChanged,
                        onSaveDisplayName = profileViewModel::saveDisplayName,
                        onCancelEditDisplayName = profileViewModel::cancelEditingDisplayName,
                        onCheckEngineStatus = profileViewModel::checkEngineStatus,
                        onOpenChats = {
                            navController.navigate(Screen.ConversationList.route)
                        },
                        onSignOut = viewModel::signOut
                    )
                }

                composable(Screen.ConversationList.route) {
                    val conversationViewModel: ConversationListViewModel = viewModel()
                    val convUiState by conversationViewModel.uiState.collectAsState()

                    ConversationListScreen(
                        uiState = convUiState,
                        onOpenUserDiscovery = conversationViewModel::openUserDiscovery,
                        onCloseUserDiscovery = conversationViewModel::closeUserDiscovery,
                        onSearchQueryChanged = conversationViewModel::onSearchQueryChanged,
                        onUserSelected = { user ->
                            conversationViewModel.startConversationWithUser(user) { convId, otherUid, otherName ->
                                navController.navigate(Screen.Chat.createRoute(convId, otherUid, otherName))
                            }
                        },
                        onConversationClicked = { convId, otherUid, otherName ->
                            navController.navigate(Screen.Chat.createRoute(convId, otherUid, otherName))
                        }
                    )
                }

                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("otherUid") { type = NavType.StringType },
                        navArgument("otherDisplayName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    val otherUid = backStackEntry.arguments?.getString("otherUid") ?: ""
                    val encodedDisplayName = backStackEntry.arguments?.getString("otherDisplayName") ?: "Cavy User"
                    val otherDisplayName = try {
                        URLDecoder.decode(encodedDisplayName, "UTF-8")
                    } catch (e: Exception) {
                        encodedDisplayName
                    }

                    val chatViewModel: ChatViewModel = viewModel()
                    val chatUiState by chatViewModel.uiState.collectAsState()

                    val currentUid = (sessionState as? AuthSessionState.Authenticated)?.user?.uid ?: ""

                    LaunchedEffect(conversationId) {
                        chatViewModel.initializeChat(conversationId, otherUid, otherDisplayName)
                    }

                    ChatScreen(
                        currentUid = currentUid,
                        uiState = chatUiState,
                        onMessageInputChanged = chatViewModel::onMessageInputChanged,
                        onSendMessage = chatViewModel::sendMessage,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
