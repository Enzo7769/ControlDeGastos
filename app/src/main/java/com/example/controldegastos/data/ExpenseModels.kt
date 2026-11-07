package com.example.controldegastos.data

import java.util.UUID

data class CreditCardItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g. "Ripley", "Falabella", "CASA", "Match"
    val categoryIcon: String = "💳", // e.g. 💳, 🛒, 🚗, 🏠, ✈️, 📱, 📄
    val isCreditCard: Boolean = true,
    val creditLimit: Double = 1000000.0, // Cupo Total
    val manualUsedQuota: Double = 0.0, // Cupo Usado ingresado manualmente por el usuario (banco)
    // Monthly amounts indexed 0, 1, 2... (e.g., 0 = Agosto, 1 = Sept, 2 = Oct, etc.)
    val monthlyAmounts: MutableMap<Int, Double> = mutableMapOf(),
    // Set of month indices that are marked as paid
    val paidMonths: MutableSet<Int> = mutableSetOf()
) {
    val totalDebt: Double
        get() = if (manualUsedQuota > 0.0) manualUsedQuota else monthlyAmounts.values.sum()

    val availableLimit: Double
        get() = (creditLimit - totalDebt).coerceAtLeast(0.0)
}

data class IncomeSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "Sueldo Principal 1", "Sueldo Principal 2"
    val monthlyAmount: Double
)

data class MonthColumn(
    val index: Int,
    val name: String // e.g., "Agosto", "Sept", "Oct", "Nov", "Dic"
)
