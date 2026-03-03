# CheckInBB - Android

CheckInBB es una aplicación nativa para Android diseñada para llevar el control y registro de la alimentación del bebé de manera rápida, intuitiva y confiable. Este proyecto es el port oficial para Android de la aplicación original en iOS (SwiftUI), manteniendo la paridad de características y un diseño moderno (incluyendo efectos *Glassmorphism*).

## 🚀 Características Principales

*   **Registro de Tomas:** Registra la hora, duración y notas adicionales de cada toma de alimento.
*   **Offline-First:** Funciona completamente sin conexión a internet. Los datos se guardan de forma local asegurando máxima privacidad y velocidad.
*   **Gestión de Alarmas y Notificaciones:** Notificaciones programadas inteligentes para recordar el próximo ciclo de alimentación basado en ventanas de tiempo configurables.
*   **Sincronización P2P (Nearby):** Permite sincronizar los registros de alimentación entre dispositivos cercanos (ej. entre padres) sin necesidad de internet usando la API Nearby Connections de Google.
*   **Widget para la Pantalla de Inicio:** Revisa el estado de la última toma e información relevante directamente desde el launcher usando (Glance).
*   **Interfaz Moderna (Material Design 3):** UI construida completamente con Jetpack Compose, con soporte para arrastrar para eliminar (Swipe-to-Dismiss) y deshacer (Undoable Deletion).

## 🛠️ Stack Tecnológico y Arquitectura

El proyecto sigue las directrices oficiales de Google para el desarrollo moderno en Android:

*   **Lenguaje:** [Kotlin](https://kotlinlang.org/)
*   **UI Declarativa:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Arquitectura:** Model-View-ViewModel (**MVVM**) y Unidirectional Data Flow (UDF).
*   **Inyección de Dependencias:** [Hilt](https://dagger.dev/hilt/)
*   **Base de Datos Local:** [Room](https://developer.android.com/training/data-storage/room)
*   **Preferencias / Ajustes:** [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
*   **Programación en Segundo Plano:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
*   **Widgets:** [Jetpack Glance](https://developer.android.com/jetpack/compose/glance)
*   **Sincronización:** [Nearby Connections API](https://developers.google.com/nearby/connections/overview)

## 📱 Requisitos

*   Android Studio Ladybug (o superior recomendado)
*   Dispositivo físico o emulador con Android 8.0 (API level 26) o superior.

## ⚙️ Estructura del Proyecto

La estructura de paquetes principal se encuentra en `app/src/main/java/com/dc/checkinbb/` y está dividida por capas:

*   `data/`: Repositorios, DAOs de Room, Entidades (BabyEntity, FeedingRecord) y DataStore.
*   `di/`: Módulos de inyección de dependencias (Hilt).
*   `sync/`: Lógica de sincronización P2P con Nearby Connections.
*   `ui/`: Interfaces gráficas (Screens) y componentes reutilizables (Components / GlassCardView) en Jetpack Compose.
*   `widget/`: Implementación del widget para la pantalla de inicio con Glance.
*   `workers/`: Trabajos en segundo plano, AlarmReceiver y NotificationManager.

## 🤝 Contribuir

Siendo un proyecto personal, actualmente se mantiene según las reglas de código estables (iOS/Android feature parity). Sin embargo, cualquier sugerencia para optimizar la UI/UX bajo las últimas guías de Material Design o solucionar problemas es bienvenida abriendo un *Issue*.
