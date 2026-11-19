package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface LifeOsRoute

@Serializable
data object DashboardRoute : LifeOsRoute

@Serializable
data object HabitsRoute : LifeOsRoute

@Serializable
data object FocusRoute : LifeOsRoute

@Serializable
data object AnalyticsRoute : LifeOsRoute

@Serializable
data object ModulesRoute : LifeOsRoute

@Serializable
data object FitnessRoute : LifeOsRoute

@Serializable
data object FinanceRoute : LifeOsRoute

@Serializable
data object GoalsRoute : LifeOsRoute

@Serializable
data object JournalRoute : LifeOsRoute

@Serializable
data object ReadingRoute : LifeOsRoute

@Serializable
data object DisciplineRoute : LifeOsRoute

@Serializable
data object SettingsRoute : LifeOsRoute
