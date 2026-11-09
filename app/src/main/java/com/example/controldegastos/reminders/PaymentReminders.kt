package com.example.controldegastos.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.controldegastos.MainActivity
import com.example.controldegastos.R
import com.example.controldegastos.data.CalendarDate
import com.example.controldegastos.data.FinanceRepository
import com.example.controldegastos.data.SettingsStore
import com.example.controldegastos.domain.DuePayment
import com.example.controldegastos.domain.FinanceCalculator
import com.example.controldegastos.util.toClp
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Recordatorios diarios de pagos por vencer (un día antes y el mismo día). */
object PaymentReminders {
    private const val WORK_NAME = "payment_reminders"
    private const val CHANNEL_ID = "payment_reminders"
    private const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_ITEM = "open_item"
    private const val REMINDER_HOUR = 9

    /** Programa (o cancela) la revisión diaria según el ajuste del usuario. */
    fun sync(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Android 13+ exige permiso para mostrar notificaciones. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun millisUntilNextRun(): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    internal fun notify(context: Context, payments: List<DuePayment>) {
        if (payments.isEmpty() || !hasPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recordatorios de pago", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisos de tarjetas y cuentas por vencer"
                }
            )
        }
        val first = payments.first()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (payments.size == 1) putExtra(EXTRA_OPEN_ITEM, first.item.id)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (payments.size == 1) "${whenText(first)} vence ${first.item.name}"
        else "${payments.size} pagos por vencer"
        val text = if (payments.size == 1) "${first.amount.toClp()} · Toca para verlo o marcarlo como pagado"
        else payments.joinToString(" · ") { "${it.item.name} ${it.amount.toClp()}" }
        val style = NotificationCompat.InboxStyle().also { s ->
            payments.forEach { s.addLine("${whenText(it)}: ${it.item.name} — ${it.amount.toClp()}") }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wallet)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(if (payments.size == 1) NotificationCompat.BigTextStyle().bigText(text) else style)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso entre la verificación y el aviso.
        }
    }

    private fun whenText(p: DuePayment): String = when (p.daysLeft) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "En ${p.daysLeft} días"
    }
}

class PaymentReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SettingsStore(applicationContext).settings.value.paymentReminders) return Result.success()
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return Result.success()
        val data = FinanceRepository.readLocal(applicationContext, uid) ?: return Result.success()
        val due = FinanceCalculator.duePayments(data, CalendarDate.today(), withinDays = 1)
        PaymentReminders.notify(applicationContext, due)
        return Result.success()
    }
}
