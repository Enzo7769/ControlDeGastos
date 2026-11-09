package com.example.controldegastos.data

import android.content.Context
import androidx.core.content.edit
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente de verdad de los datos de UN usuario.
 *
 * - Guarda una copia local (SharedPreferences por usuario) para abrir la app al instante y sin internet.
 * - Escucha el documento en Firestore para sincronizar entre dispositivos.
 * - Gana la versión con `updatedAt` más reciente.
 * - Migra automáticamente los datos del formato antiguo (local o en la nube).
 */
class FinanceRepository(
    context: Context,
    val userId: String,
    /** Importar los datos de la versión anterior guardados en este teléfono (solo para la sesión que venía abierta). */
    private val importLegacy: Boolean,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("finance_$userId", Context.MODE_PRIVATE)
    private val docRef = firestore.collection("users").document(userId).collection("data").document("financial_info")

    private val _data = MutableStateFlow(loadLocal())
    val data: StateFlow<FinanceData> = _data.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.SYNCING)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var registration: ListenerRegistration? = null

    fun start() {
        if (registration != null) return
        registration = docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "No se pudo sincronizar con la nube", error)
                _syncState.value = SyncState.OFFLINE
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            val current = _data.value
            if (snapshot.exists()) {
                val remoteUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
                val remote = FinanceCodec.decode(snapshot.getString("payload"))
                    ?: FinanceCodec.migrateLegacy(
                        cardsJson = snapshot.getString("cardsJson"),
                        incomesJson = snapshot.getString("incomesJson"),
                        monthsJson = snapshot.getString("monthsJson"),
                        fallbackStart = YearMonth.now(),
                        updatedAt = remoteUpdatedAt,
                    )
                when {
                    remote != null && remote.updatedAt > current.updatedAt -> {
                        _data.value = remote
                        saveLocal(remote)
                    }
                    current.updatedAt > (remote?.updatedAt ?: 0L) && !current.isEmpty -> push(current)
                }
            } else if (!snapshot.metadata.isFromCache && !current.isEmpty) {
                // Primera vez en la nube: subir lo que hay localmente.
                push(current)
            }
            _syncState.value = if (snapshot.metadata.isFromCache) SyncState.OFFLINE
            else if (snapshot.metadata.hasPendingWrites()) SyncState.SYNCING
            else SyncState.SYNCED
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    /** Aplica un cambio, lo guarda localmente y lo envía a la nube. */
    fun update(transform: (FinanceData) -> FinanceData) {
        val next = transform(_data.value).copy(updatedAt = System.currentTimeMillis())
        _data.value = next
        saveLocal(next)
        push(next)
    }

    private fun push(data: FinanceData) {
        _syncState.value = SyncState.SYNCING
        docRef.set(
            mapOf(
                "schemaVersion" to FinanceCodec.SCHEMA_VERSION,
                "payload" to FinanceCodec.encode(data),
                "updatedAt" to data.updatedAt,
            )
        )
            .addOnSuccessListener { _syncState.value = SyncState.SYNCED }
            .addOnFailureListener {
                Log.w(TAG, "Error al guardar en la nube", it)
                _syncState.value = SyncState.OFFLINE
            }
    }

    private fun saveLocal(data: FinanceData) {
        prefs.edit { putString(KEY_DATA, FinanceCodec.encode(data)) }
    }

    private fun loadLocal(): FinanceData {
        FinanceCodec.decode(prefs.getString(KEY_DATA, null))?.let { return it }
        return migrateLegacyLocal() ?: FinanceData()
    }

    /**
     * La versión anterior guardaba todo en "ControlDeGastosPrefs" sin separar por usuario.
     * Solo se importan si el usuario ya tenía la sesión abierta al actualizar (son sus datos);
     * en cualquier caso se borran después, para que nunca pasen a otra cuenta.
     * Se usa updatedAt = 1 para que, si la nube ya tiene datos, prevalezcan los de la nube.
     */
    private fun migrateLegacyLocal(): FinanceData? {
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacy.getBoolean(KEY_LEGACY_MIGRATED, false)) return null
        val migrated = if (importLegacy) {
            FinanceCodec.migrateLegacy(
                cardsJson = legacy.getString("saved_cards", null),
                incomesJson = legacy.getString("saved_incomes", null),
                monthsJson = legacy.getString("saved_months", null),
                fallbackStart = YearMonth.now(),
                updatedAt = 1L,
            )
        } else null
        legacy.edit {
            remove("saved_cards")
            remove("saved_incomes")
            remove("saved_months")
            putBoolean(KEY_LEGACY_MIGRATED, true)
        }
        migrated?.let(::saveLocal)
        return migrated
    }

    enum class SyncState { SYNCING, SYNCED, OFFLINE }

    companion object {
        /** Lee los datos guardados en el teléfono sin conectarse a la nube (para tareas en segundo plano). */
        fun readLocal(context: Context, userId: String): FinanceData? = FinanceCodec.decode(
            context.applicationContext.getSharedPreferences("finance_$userId", Context.MODE_PRIVATE).getString(KEY_DATA, null)
        )

        private const val TAG = "FinanceRepository"
        private const val KEY_DATA = "finance_data_v2"
        const val LEGACY_PREFS = "ControlDeGastosPrefs"
        private const val KEY_LEGACY_MIGRATED = "legacy_migrated"
    }
}
