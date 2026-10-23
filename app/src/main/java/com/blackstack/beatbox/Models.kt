package com.blackstack.beatbox

import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class AppLanguage(val label: String, val code: String) {
    TR("Türkçe", "tr"),
    EN("English", "en"),
    ES("Español", "es"),
    FR("Français", "fr"),
    DE("Deutsch", "de"),
    IT("Italiano", "it"),
    PT("Português", "pt"),
    RU("Русский", "ru"),
    JA("日本語", "ja"),
    ZH("中文", "zh"),
    AR("العربية", "ar"),
    KO("한국어", "ko")
}

enum class AppTheme(
    val label: String, 
    val labelTR: String, 
    val isDark: Boolean, 
    val previewColor: Color,
    val isGradient: Boolean = true,
    val gradientColors: List<Color> = emptyList()
) {
    // Modern, Sweet & Vibrant Themes
    DYNAMIC("Dynamic Art", "Dinamik Albüm", true, Color(0xFF00E5FF), true, listOf(Color(0xFF0C0D15), Color(0xFF16192B), Color(0xFF0C0D15))),
    AMOLED("OLED Black", "Zifiri Siyah", true, Color(0xFF00E5FF), true, listOf(Color(0xFF000000), Color(0xFF09090D), Color(0xFF000000))),
    DARK("Midnight", "Gece Yarısı", true, Color(0xFF9C27B0), true, listOf(Color(0xFF0C0A1A), Color(0xFF1B1433), Color(0xFF0A0714))),
    NEON_CYBER("Cyberpunk", "Siberpunk", true, Color(0xFF00F5D4), true, listOf(Color(0xFF180030), Color(0xFF2D0052), Color(0xFF00222B))),
    ROSE_GOLD("Rose Gold", "Roz Altın", true, Color(0xFFF2CC8F), true, listOf(Color(0xFF21171C), Color(0xFF382730), Color(0xFF171013)))
}

enum class AuthProvider(val label: String) {
    EMAIL("E-posta"),
    PHONE("Telefon"),
    GUEST("Misafir")
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String = "BeatBox Dinleyicisi",
    val email: String = "",
    val phoneNumber: String = "",
    val bio: String = "Müzik ruhun gıdasıdır ✨",
    val avatarId: Int = 0,
    val joinedDate: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false,
    val authProvider: AuthProvider = AuthProvider.GUEST,
    val isPremium: Boolean = true
)

data class Artist(
    val name: String,
    val id: String? = null
)

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumId: Long,
    val folderName: String,
    val dateAdded: Long,
    val isOnline: Boolean = false,
    val artworkUrl: String? = null,
    val isExplicit: Boolean = false,
    val albumArtist: String? = null
)

fun String.parseTimeMs(): Long {
    val parts = this.split(":").mapNotNull { it.trim().toLongOrNull() }
    return when (parts.size) {
        2 -> (parts[0] * 60 + parts[1]) * 1000L
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        else -> this.toLongOrNull() ?: 0L
    }
}

data class Playlist(
    val name: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val songIds: MutableList<Long> = mutableListOf(),
    val isSmart: Boolean = false,
    val isArtistPlaylist: Boolean = false,
    val artistName: String? = null
)

fun groupSongsByArtist(songs: List<AudioFile>, unknownLabel: String = "Unknown Artist"): Map<String, List<AudioFile>> {
    return songs.groupBy { it.artist.trim().ifBlank { unknownLabel } }
}

enum class EQPreset(val label: String, val labelTR: String, val desc: String, val descTR: String) {
    NONE("Normal", "Normal", "Original balanced audio profile", "Orijinal dengeli ses profili"),
    BASS_BOOST("Bass Boost", "Bas Güçlendirme", "Deep low-end punch & enhanced rhythm", "Derin bas vuruşları ve güçlü ritim"),
    VOCAL("Vocal Boost", "Vokal", "Crystal clear vocals & speech clarity", "Net ve ön planda vokaller"),
    TREBLE("Treble Boost", "Tiz", "Bright highs & crisp instrument details", "Parlak tizler ve enstrüman detayları"),
    LOUDNESS("Loudness", "Yüksek Ses", "Maximized volume & rich fullness", "Maksimum ses seviyesi ve dolgunluk");
}

enum class SortOption { DATE, NAME }

class Strings(val lang: AppLanguage) {
    // Multi-language support: EN, TR, ES, FR, DE, IT, PT, RU, JA, ZH, AR, KO
    private val translations = mapOf(
        "app_name" to mapOf(
            "en" to "BeatBox", "tr" to "BeatBox", "es" to "BeatBox", "fr" to "BeatBox",
            "de" to "BeatBox", "it" to "BeatBox", "pt" to "BeatBox", "ru" to "BeatBox",
            "ja" to "BeatBox", "zh" to "BeatBox", "ar" to "BeatBox", "ko" to "BeatBox"
        ),
        "library" to mapOf(
            "en" to "Library", "tr" to "Kütüphane", "es" to "Biblioteca", "fr" to "Bibliothèque",
            "de" to "Bibliothek", "it" to "Biblioteca", "pt" to "Biblioteca", "ru" to "Библиотека",
            "ja" to "ライブラリ", "zh" to "音乐库", "ar" to "المكتبة", "ko" to "라이브러리"
        ),
        "playlists" to mapOf(
            "en" to "Playlists", "tr" to "Çalma Listeleri", "es" to "Listas", "fr" to "Listes",
            "de" to "Wiedergabelisten", "it" to "Playlist", "pt" to "Playlists", "ru" to "Плейлисты",
            "ja" to "プレイリスト", "zh" to "播放列表", "ar" to "قوائم التشغيل", "ko" to "재생목록"
        ),
        "settings" to mapOf(
            "en" to "Settings", "tr" to "Ayarlar", "es" to "Ajustes", "fr" to "Paramètres",
            "de" to "Einstellungen", "it" to "Impostazioni", "pt" to "Configurações", "ru" to "Настройки",
            "ja" to "設定", "zh" to "设置", "ar" to "الإعدادات", "ko" to "설정"
        ),
        "search_hint" to mapOf(
            "en" to "Search songs, artists...", "tr" to "Şarkı, sanatçı ara...", "es" to "Buscar canciones, artistas...",
            "fr" to "Rechercher des chansons, artistes...", "de" to "Lieder, Künstler suchen...",
            "it" to "Cerca canzoni, artisti...", "pt" to "Pesquisar músicas, artistas...",
            "ru" to "Поиск песен, исполнителей...", "ja" to "曲、アーティストを検索...",
            "zh" to "搜索歌曲、艺术家...", "ar" to "البحث عن الأغاني والفنانين...", "ko" to "노래, 아티스트 검색..."
        ),
        "empty_library" to mapOf(
            "en" to "No songs found", "tr" to "Şarkı bulunamadı", "es" to "No se encontraron canciones",
            "fr" to "Aucune chanson trouvée", "de" to "Keine Lieder gefunden", "it" to "Nessuna canzone trovata",
            "pt" to "Nenhuma música encontrada", "ru" to "Песни не найдены", "ja" to "曲が見つかりません",
            "zh" to "未找到歌曲", "ar" to "لم يتم العثور على أغاني", "ko" to "노래를 찾을 수 없습니다"
        ),
        "empty_playlists" to mapOf(
            "en" to "No playlists yet", "tr" to "Henüz çalma listesi yok", "es" to "Aún no hay listas",
            "fr" to "Pas encore de listes", "de" to "Noch keine Wiedergabelisten", "it" to "Nessuna playlist ancora",
            "pt" to "Ainda sem playlists", "ru" to "Пока нет плейлистов", "ja" to "まだプレイリストがありません",
            "zh" to "还没有播放列表", "ar" to "لا توجد قوائم تشغيل بعد", "ko" to "아직 재생목록이 없습니다"
        ),
        "favorites" to mapOf(
            "en" to "Favorites", "tr" to "Favoriler", "es" to "Favoritos", "fr" to "Favoris",
            "de" to "Favoriten", "it" to "Preferiti", "pt" to "Favoritos", "ru" to "Избранное",
            "ja" to "お気に入り", "zh" to "收藏", "ar" to "المفضلة", "ko" to "즐겨찾기"
        ),
        "artist_playlists" to mapOf(
            "en" to "Artist Playlists", "tr" to "Sanatçı Çalma Listeleri", "es" to "Listas de artistas", "fr" to "Listes d'artistes",
            "de" to "Künstler-Playlists", "it" to "Playlist artista", "pt" to "Playlists de artistas", "ru" to "Плейлисты исполнителей",
            "ja" to "アーティストプレイリスト", "zh" to "艺术家播放列表", "ar" to "قوائم تشغيل الفنانين", "ko" to "아티스트 재생목록"
        ),
        "create_artist_playlists" to mapOf(
            "en" to "Auto-Create Artist Playlists", "tr" to "Sanatçı Listelerini Otomatik Oluştur", "es" to "Crear listas de artistas automáticamente",
            "fr" to "Créer automatiquement des listes d'artistes", "de" to "Künstler-Playlists automatisch erstellen", "it" to "Crea playlist artista automaticamente",
            "pt" to "Criar playlists de artistas automaticamente", "ru" to "Автосоздание плейлистов исполнителей", "ja" to "アーティストプレイリストを自動作成",
            "zh" to "自动创建艺术家播放列表", "ar" to "إنشاء قوائم تشغيل الفنانين تلقائياً", "ko" to "아티스트 재생목록 자동 생성"
        ),
        "artist_playlists_created" to mapOf(
            "en" to "Artist playlists created!", "tr" to "Sanatçı çalma listeleri oluşturuldu!", "es" to "¡Listas de artistas creadas!",
            "fr" to "Listes d'artistes créées !", "de" to "Künstler-Playlists erstellt!", "it" to "Playlist artista create!",
            "pt" to "Playlists de artistas criadas!", "ru" to "Плейлисты исполнителей созданы!", "ja" to "アーティストプレイリストを作成しました！",
            "zh" to "已创建艺术家播放列表！", "ar" to "تم إنشاء قوائم تشغيل الفنانين!", "ko" to "아티스트 재생목록이 생성되었습니다!"
        ),
        "sleep_timer" to mapOf(
            "en" to "Sleep Timer", "tr" to "Uyku Zamanlayıcısı", "es" to "Temporizador", "fr" to "Minuteur",
            "de" to "Schlaf-Timer", "it" to "Timer", "pt" to "Temporizador", "ru" to "Таймер сна",
            "ja" to "スリープタイマー", "zh" to "睡眠定时器", "ar" to "مؤقت النوم", "ko" to "수면 타이머"
        ),
        "equalizer" to mapOf(
            "en" to "Equalizer", "tr" to "Ekolayzer", "es" to "Ecualizador", "fr" to "Égaliseur",
            "de" to "Equalizer", "it" to "Equalizzatore", "pt" to "Equalizador", "ru" to "Эквалайзер",
            "ja" to "イコライザー", "zh" to "均衡器", "ar" to "المعادل", "ko" to "이퀄라이저"
        ),
        "theme" to mapOf(
            "en" to "Theme", "tr" to "Tema", "es" to "Tema", "fr" to "Thème",
            "de" to "Design", "it" to "Tema", "pt" to "Tema", "ru" to "Тема",
            "ja" to "テーマ", "zh" to "主题", "ar" to "المظهر", "ko" to "테마"
        ),
        "language" to mapOf(
            "en" to "Language", "tr" to "Dil", "es" to "Idioma", "fr" to "Langue",
            "de" to "Sprache", "it" to "Lingua", "pt" to "Idioma", "ru" to "Язык",
            "ja" to "言語", "zh" to "语言", "ar" to "اللغة", "ko" to "언어"
        ),
        "shake_to_shuffle" to mapOf(
            "en" to "Shake to Shuffle", "tr" to "Salla ve Karıştır", "es" to "Agitar para mezclar",
            "fr" to "Secouer pour mélanger", "de" to "Schütteln zum Mischen", "it" to "Agita per mescolare",
            "pt" to "Agite para embaralhar", "ru" to "Встряхните для перемешивания", "ja" to "シャッフルするには振る",
            "zh" to "摇动以随机播放", "ar" to "هز للخلط", "ko" to "흔들어서 섞기"
        ),
        "crossfade" to mapOf(
            "en" to "Crossfade", "tr" to "Geçiş Efekti", "es" to "Fundido cruzado", "fr" to "Fondu enchaîné",
            "de" to "Überblendung", "it" to "Dissolvenza", "pt" to "Crossfade", "ru" to "Кроссфейд",
            "ja" to "クロスフェード", "zh" to "淡入淡出", "ar" to "التلاشي المتقاطع", "ko" to "크로스페이드"
        ),
        "keep_screen_on" to mapOf(
            "en" to "Keep Screen On", "tr" to "Ekranı Asla Kapatma", "es" to "Mantener pantalla encendida",
            "fr" to "Garder l'écran allumé", "de" to "Bildschirm anlassen", "it" to "Tieni schermo acceso",
            "pt" to "Manter tela ligada", "ru" to "Не выключать экран", "ja" to "画面 ON を維持",
            "zh" to "保持屏幕常亮", "ar" to "إبقاء الشاشة قيد التشغيل", "ko" to "화면 켜짐 유지"
        ),
        "folders" to mapOf(
            "en" to "Folders", "tr" to "Klasörler", "es" to "Carpetas", "fr" to "Dossiers",
            "de" to "Ordner", "it" to "Cartelle", "pt" to "Pastas", "ru" to "Папки",
            "ja" to "フォルダ", "zh" to "文件夹", "ar" to "المجلدات", "ko" to "폴더"
        ),
        "about" to mapOf(
            "en" to "About", "tr" to "Hakkında", "es" to "Acerca de", "fr" to "À propos",
            "de" to "Über", "it" to "Informazioni", "pt" to "Sobre", "ru" to "О программе",
            "ja" to "について", "zh" to "关于", "ar" to "حول", "ko" to "정보"
        ),
        "clear_cache" to mapOf(
            "en" to "Clear Photo Cache", "tr" to "Görsel Önbelleğini Temizle", "es" to "Borrar caché de fotos", "fr" to "Effacer le cache des photos",
            "de" to "Foto-Cache leeren", "it" to "Svuota cache foto", "pt" to "Limpar cache de fotos", "ru" to "Очистить кэш фото",
            "ja" to "写真キャッシュを消去", "zh" to "清除照片缓存", "ar" to "مسح ذاكرة التخزين المؤقت للصور", "ko" to "사진 캐시 삭제"
        ),
        "cache_cleared" to mapOf(
            "en" to "Photo cache cleared!", "tr" to "Görsel önbelleği temizlendi!", "es" to "¡Caché de fotos borrada!", "fr" to "Cache des photos effacé !",
            "de" to "Foto-Cache geleert!", "it" to "Cache foto svuotata!", "pt" to "Cache de fotos limpo!", "ru" to "Кэш фото очищен!",
            "ja" to "写真キャッシュを消去しました！", "zh" to "照片缓存已清除！", "ar" to "تم مسح ذاكرة التخزين المؤقت للصور!", "ko" to "사진 캐시가 삭제되었습니다!"
        ),
        "appearance" to mapOf(
            "en" to "Appearance", "tr" to "Görünüm", "es" to "Apariencia", "fr" to "Apparence",
            "de" to "Aussehen", "it" to "Aspetto", "pt" to "Aparência", "ru" to "Внешний вид",
            "ja" to "外観", "zh" to "外观", "ar" to "المظهر", "ko" to "외관"
        ),
        "tempo_visualization" to mapOf(
            "en" to "Tempo Visualization", "tr" to "Tempo Görselleştirme", "es" to "Visualización de tempo",
            "fr" to "Visualisation du tempo", "de" to "Tempo-Visualisierung", "it" to "Visualizzazione tempo",
            "pt" to "Visualização de tempo", "ru" to "Визуализация темпа", "ja" to "テンポの視覚化",
            "zh" to "节奏可视化", "ar" to "تصور الإيقاع", "ko" to "템포 시각화"
        ),
        "performance_mode" to mapOf(
            "en" to "Performance Mode", "tr" to "Performans Modu", "es" to "Modo rendimiento",
            "fr" to "Mode performance", "de" to "Leistungsmodus", "it" to "Modalità prestazioni",
            "pt" to "Modo de desempenho", "ru" to "Режим производительности", "ja" to "パフォーマンスモード",
            "zh" to "性能模式", "ar" to "وضع الأداء", "ko" to "성능 모드"
        ),
        "audio" to mapOf(
            "en" to "Audio", "tr" to "Ses", "es" to "Audio", "fr" to "Audio",
            "de" to "Audio", "it" to "Audio", "pt" to "Áudio", "ru" to "Аудио",
            "ja" to "オーディオ", "zh" to "音频", "ar" to "الصوت", "ko" to "오디오"
        ),
        "general" to mapOf(
            "en" to "General", "tr" to "Genel", "es" to "General", "fr" to "Général",
            "de" to "Allgemein", "it" to "Generale", "pt" to "Geral", "ru" to "Общие",
            "ja" to "一般", "zh" to "常规", "ar" to "عام", "ko" to "일반"
        ),
        "personalization" to mapOf(
            "en" to "Personalization & Summary", "tr" to "Kişiselleştirme & Özet", "es" to "Personalización y resumen",
            "fr" to "Personnalisation et résumé", "de" to "Personalisierung & Zusammenfassung", "it" to "Personalizzazione e riepilogo",
            "pt" to "Personalização e resumo", "ru" to "Персонализация и сводка", "ja" to "パーソナライズと概要",
            "zh" to "个性化与总结", "ar" to "التخصيص والملخص", "ko" to "개인화 및 요약"
        ),
        "wrapped_title" to mapOf(
            "en" to "BeatBox Wrapped (Music Summary)", "tr" to "BeatBox Wrapped (Müzik Özetim)", "es" to "BeatBox Wrapped (Resumen musical)",
            "fr" to "BeatBox Wrapped (Résumé musical)", "de" to "BeatBox Wrapped (Musikzusammenfassung)", "it" to "BeatBox Wrapped (Riepilogo musicale)",
            "pt" to "BeatBox Wrapped (Resumo musical)", "ru" to "BeatBox Wrapped (Музыкальный итог)", "ja" to "BeatBox Wrapped (音楽のまとめ)",
            "zh" to "BeatBox Wrapped (音乐总结)", "ar" to "BeatBox Wrapped (ملخص الموسيقى)", "ko" to "BeatBox Wrapped (음악 요약)"
        ),
        "wrapped_desc" to mapOf(
            "en" to "View your listening statistics & habits", "tr" to "Dinleme istatistiklerinizi ve alışkanlıklarınızı görün",
            "es" to "Ver tus estadísticas y hábitos de escucha", "fr" to "Voir vos statistiques et habitudes d'écoute",
            "de" to "Sehen Sie Ihre Hörstatistiken und Gewohnheiten", "it" to "Visualizza le tue statistiche e abitudini di ascolto",
            "pt" to "Veja suas estatísticas e hábitos de audição", "ru" to "Просмотр статистики и привычек прослушивания",
            "ja" to "リスニングの統計と習慣を見る", "zh" to "查看您的收听统计和习惯", "ar" to "عرض إحصائيات وعادات الاستماع الخاصة بك",
            "ko" to "청취 통계 및 습관 보기"
        ),
        "listening_history" to mapOf(
            "en" to "Listening History", "tr" to "Dinleme Geçmişi", "es" to "Historial de escucha",
            "fr" to "Historique d'écoute", "de" to "Hörverlauf", "it" to "Cronologia di ascolto",
            "pt" to "Histórico de audição", "ru" to "История прослушивания", "ja" to "再生履歴",
            "zh" to "收听历史", "ar" to "سجل الاستماع", "ko" to "청취 기록"
        ),
        "history_desc" to mapOf(
            "en" to "Recently played tracks", "tr" to "Son dinlenen şarkılar", "es" to "Pistas reproducidas recientemente",
            "fr" to "Pistes récemment jouées", "de" to "Kürzlich abgespielte Titel", "it" to "Tracce riprodotte di recente",
            "pt" to "Músicas reproduzidas recentemente", "ru" to "Недавно прослушанные треки", "ja" to "最近再生した曲",
            "zh" to "最近播放的曲目", "ar" to "المسارات التشغيل الأخيرة", "ko" to "최근 재생된 트랙"
        ),
        "good_morning" to mapOf(
            "en" to "Good morning", "tr" to "Günaydın", "es" to "Buenos días", "fr" to "Bonjour",
            "de" to "Guten Morgen", "it" to "Buongiorno", "pt" to "Bom dia", "ru" to "Доброе утро",
            "ja" to "おはようございます", "zh" to "早上好", "ar" to "صباح الخير", "ko" to "좋은 아침입니다"
        ),
        "good_afternoon" to mapOf(
            "en" to "Good afternoon", "tr" to "İyi günler", "es" to "Buenas tardes", "fr" to "Bon après-midi",
            "de" to "Guten Tag", "it" to "Buon pomeriggio", "pt" to "Boa tarde", "ru" to "Добрый день",
            "ja" to "こんにちは", "zh" to "下午好", "ar" to "طاب يومك", "ko" to "좋은 오후입니다"
        ),
        "good_evening" to mapOf(
            "en" to "Good evening", "tr" to "İyi akşamlar", "es" to "Buenas noches", "fr" to "Bonsoir",
            "de" to "Guten Abend", "it" to "Buonasera", "pt" to "Boa noite", "ru" to "Добрый вечер",
            "ja" to "こんばんは", "zh" to "晚上好", "ar" to "مساء الخير", "ko" to "좋은 저녁입니다"
        ),
        "home_subtitle" to mapOf(
            "en" to "What do you want to listen to?", "tr" to "Bugün ne dinleyeceksin?", "es" to "¿Qué quieres escuchar?",
            "fr" to "Que veux-tu écouter ?", "de" to "Was möchtest du hören?", "it" to "Cosa vuoi ascoltare?",
            "pt" to "O que você quer ouvir?", "ru" to "Что хочешь послушать?", "ja" to "何を聴きたいですか？",
            "zh" to "今天想听什么？", "ar" to "ماذا تريد أن تسمع؟", "ko" to "무엇을 듣고 싶나요?"
        ),
        "continue_listening" to mapOf(
            "en" to "Continue listening", "tr" to "Kaldığın yerden", "es" to "Continuar escuchando",
            "fr" to "Reprendre l'écoute", "de" to "Weiterhören", "it" to "Riprendi l'ascolto",
            "pt" to "Continuar ouvindo", "ru" to "Продолжить прослушивание", "ja" to "続きを聴く",
            "zh" to "继续收听", "ar" to "متابعة الاستماع", "ko" to "계속 듣기"
        ),
        "recently_played" to mapOf(
            "en" to "Recently played", "tr" to "Son çalınanlar", "es" to "Reproducidas recientemente",
            "fr" to "Récemment jouées", "de" to "Kürzlich gespielt", "it" to "Riprodotte di recente",
            "pt" to "Reproduzidas recentemente", "ru" to "Недавно игравшие", "ja" to "最近再生した曲",
            "zh" to "最近播放", "ar" to "تم تشغيلها مؤخرًا", "ko" to "최근 재생"
        ),
        "for_you" to mapOf(
            "en" to "For you", "tr" to "Senin için", "es" to "Para ti", "fr" to "Pour toi",
            "de" to "Für dich", "it" to "Per te", "pt" to "Para você", "ru" to "Для тебя",
            "ja" to "あなたのために", "zh" to "为你推荐", "ar" to "لك", "ko" to "당신을 위해"
        ),
        "play" to mapOf(
            "en" to "Play", "tr" to "Oynat", "es" to "Reproducir", "fr" to "Lecture",
            "de" to "Abspielen", "it" to "Riproduci", "pt" to "Reproduzir", "ru" to "Воспроизвести",
            "ja" to "再生", "zh" to "播放", "ar" to "تشغيل", "ko" to "재생"
        ),
        "pause" to mapOf(
            "en" to "Pause", "tr" to "Duraklat", "es" to "Pausa", "fr" to "Pause",
            "de" to "Pause", "it" to "Pausa", "pt" to "Pausar", "ru" to "Пауза",
            "ja" to "一時停止", "zh" to "暂停", "ar" to "إيقاف مؤقت", "ko" to "일시정지"
        ),
        "edit" to mapOf(
            "en" to "Edit", "tr" to "Düzenle", "es" to "Editar", "fr" to "Modifier",
            "de" to "Bearbeiten", "it" to "Modifica", "pt" to "Editar", "ru" to "Изменить",
            "ja" to "編集", "zh" to "编辑", "ar" to "تعديل", "ko" to "편집"
        ),
        "login" to mapOf(
            "en" to "Sign In", "tr" to "Giriş Yap", "es" to "Iniciar sesión", "fr" to "Se connecter",
            "de" to "Anmelden", "it" to "Accedi", "pt" to "Entrar", "ru" to "Войти",
            "ja" to "サインイン", "zh" to "登录", "ar" to "تسجيل الدخول", "ko" to "로그인"
        ),
        "login_prompt" to mapOf(
            "en" to "Sign in or register", "tr" to "Kayıt olun veya giriş yapın", "es" to "Regístrate o inicia sesión",
            "fr" to "Inscrivez-vous ou connectez-vous", "de" to "Registrieren oder anmelden", "it" to "Registrati o accedi",
            "pt" to "Registre-se ou faça login", "ru" to "Зарегистрируйтесь или войдите", "ja" to "登録またはサインイン",
            "zh" to "注册或登录", "ar" to "سجل أو قم بتسجيل الدخول", "ko" to "가입 또는 로그인"
        ),
        "ok" to mapOf(
            "en" to "OK", "tr" to "Tamam", "es" to "OK", "fr" to "OK",
            "de" to "OK", "it" to "OK", "pt" to "OK", "ru" to "ОК",
            "ja" to "OK", "zh" to "确定", "ar" to "موافق", "ko" to "확인"
        ),
        "cancel" to mapOf(
            "en" to "Cancel", "tr" to "İptal", "es" to "Cancelar", "fr" to "Annuler",
            "de" to "Abbrechen", "it" to "Annulla", "pt" to "Cancelar", "ru" to "Отмена",
            "ja" to "キャンセル", "zh" to "取消", "ar" to "إلغاء", "ko" to "취소"
        ),
        "manual_search" to mapOf(
            "en" to "Manual Search", "tr" to "Manuel Ara", "es" to "Búsqueda manual",
            "fr" to "Recherche manuelle", "de" to "Manuelle Suche", "it" to "Ricerca manuale",
            "pt" to "Pesquisa manual", "ru" to "Ручной поиск", "ja" to "手動検索",
            "zh" to "手动搜索", "ar" to "بحث يدوي", "ko" to "수동 검색"
        ),
        "track_name" to mapOf(
            "en" to "Track Name", "tr" to "Şarkı Adı", "es" to "Nombre de pista",
            "fr" to "Nom de la piste", "de" to "Titelname", "it" to "Nome traccia",
            "pt" to "Nome da faixa", "ru" to "Название трека", "ja" to "曲名",
            "zh" to "曲名", "ar" to "اسم المسار", "ko" to "트랙 이름"
        ),
        "artist" to mapOf(
            "en" to "Artist", "tr" to "Sanatçı", "es" to "Artista",
            "fr" to "Artiste", "de" to "Künstler", "it" to "Artista",
            "pt" to "Artista", "ru" to "Исполнитель", "ja" to "アーティスト",
            "zh" to "艺术家", "ar" to "الفنان", "ko" to "아티스트"
        ),
        "duration" to mapOf(
            "en" to "Duration", "tr" to "Süre", "es" to "Duración",
            "fr" to "Durée", "de" to "Dauer", "it" to "Durata",
            "pt" to "Duração", "ru" to "Длительность", "ja" to "再生時間",
            "zh" to "时长", "ar" to "المدة", "ko" to "재생 시간"
        ),
        "folder" to mapOf(
            "en" to "Folder", "tr" to "Klasör", "es" to "Carpeta",
            "fr" to "Dossier", "de" to "Ordner", "it" to "Cartella",
            "pt" to "Pasta", "ru" to "Папка", "ja" to "フォルダ",
            "zh" to "文件夹", "ar" to "المجلد", "ko" to "폴더"
        ),
        "search" to mapOf(
            "en" to "Search", "tr" to "Ara", "es" to "Buscar",
            "fr" to "Rechercher", "de" to "Suchen", "it" to "Cerca",
            "pt" to "Pesquisar", "ru" to "Поиск", "ja" to "検索",
            "zh" to "搜索", "ar" to "بحث", "ko" to "검색"
        ),
        "close" to mapOf(
            "en" to "Close", "tr" to "Kapat", "es" to "Cerrar",
            "fr" to "Fermer", "de" to "Schließen", "it" to "Chiudi",
            "pt" to "Fechar", "ru" to "Закрыть", "ja" to "閉じる",
            "zh" to "关闭", "ar" to "إغلاق", "ko" to "닫기"
        ),
        "song_deleted" to mapOf(
            "en" to "Song deleted", "tr" to "Şarkı silindi", "es" to "Canción eliminada",
            "fr" to "Chanson supprimée", "de" to "Lied gelöscht", "it" to "Canzone eliminata",
            "pt" to "Música excluída", "ru" to "Песня удалена", "ja" to "曲が削除されました",
            "zh" to "歌曲已删除", "ar" to "تم حذف الأغنية", "ko" to "노래가 삭제되었습니다"
        ),
        "deletion_cancelled" to mapOf(
            "en" to "Deletion cancelled", "tr" to "Silme işlemi iptal edildi", "es" to "Eliminación cancelada",
            "fr" to "Suppression annulée", "de" to "Löschen abgebrochen", "it" to "Eliminazione annullata",
            "pt" to "Exclusão cancelada", "ru" to "Удаление отменено", "ja" to "削除がキャンセルされました",
            "zh" to "已取消删除", "ar" to "تم إلغاء الحذف", "ko" to "삭제가 취소되었습니다"
        ),
        "start" to mapOf(
            "en" to "Start", "tr" to "Başlat", "es" to "Iniciar", "fr" to "Démarrer",
            "de" to "Starten", "it" to "Avvia", "pt" to "Iniciar", "ru" to "Начать",
            "ja" to "開始", "zh" to "开始", "ar" to "ابدأ", "ko" to "시작"
        ),
        "minutes" to mapOf(
            "en" to "minutes", "tr" to "dakika", "es" to "minutos", "fr" to "minutes",
            "de" to "Minuten", "it" to "minuti", "pt" to "minutos", "ru" to "минут",
            "ja" to "分", "zh" to "分钟", "ar" to "دقائق", "ko" to "분"
        ),
        "home" to mapOf(
            "en" to "Home", "tr" to "Ana Sayfa", "es" to "Inicio", "fr" to "Accueil",
            "de" to "Startseite", "it" to "Home", "pt" to "Início", "ru" to "Главная",
            "ja" to "ホーム", "zh" to "主页", "ar" to "الرئيسية", "ko" to "홈"
        ),
        "songs_count" to mapOf(
            "en" to "Songs", "tr" to "Şarkı", "es" to "Canciones", "fr" to "Chansons",
            "de" to "Lieder", "it" to "Canzoni", "pt" to "Músicas", "ru" to "Песни",
            "ja" to "曲", "zh" to "歌曲", "ar" to "أغاني", "ko" to "노래"
        ),
        "shuffle" to mapOf(
            "en" to "Shuffle", "tr" to "Karıştır", "es" to "Aleatorio", "fr" to "Aléatoire",
            "de" to "Zufällig", "it" to "Casuale", "pt" to "Aleatório", "ru" to "Случайно",
            "ja" to "シャッフル", "zh" to "随机播放", "ar" to "عشوائي", "ko" to "셔플"
        ),
        "delete" to mapOf(
            "en" to "Delete", "tr" to "Sil", "es" to "Eliminar", "fr" to "Supprimer",
            "de" to "Löschen", "it" to "Elimina", "pt" to "Excluir", "ru" to "Удалить",
            "ja" to "削除", "zh" to "删除", "ar" to "حذف", "ko" to "삭제"
        ),
        "same_artist_songs" to mapOf(
            "en" to "Same Artist Songs", "tr" to "Aynı Sanatçının Şarkıları", "es" to "Canciones del mismo artista", "fr" to "Chansons du même artiste",
            "de" to "Songs desselben Künstlers", "it" to "Canzoni dello stesso artista", "pt" to "Músicas do mesmo artista", "ru" to "Песни того же исполнителя",
            "ja" to "同じアーティストの曲", "zh" to "同一艺术家的歌曲", "ar" to "أغاني نفس الفنان", "ko" to "같은 아티스트의 노래"
        ),
        "welcome" to mapOf(
            "en" to "Welcome to BeatBox", "tr" to "BeatBox'a Hoş Geldiniz", "es" to "Bienvenido a BeatBox",
            "fr" to "Bienvenue sur BeatBox", "de" to "Willkommen bei BeatBox", "it" to "Benvenuto su BeatBox",
            "pt" to "Bem-vindo ao BeatBox", "ru" to "Добро пожаловать в BeatBox", "ja" to "BeatBoxへようこそ",
            "zh" to "欢迎使用BeatBox", "ar" to "مرحبا بك في BeatBox", "ko" to "BeatBox에 오신 것을 환영합니다"
        ),
        "welcome_desc" to mapOf(
            "en" to "Experience music like never before.", "tr" to "Müziği daha önce hiç olmadığı gibi deneyimleyin.",
            "es" to "Experimenta la música como nunca antes.", "fr" to "Découvrez la musique comme jamais auparavant.",
            "de" to "Erleben Sie Musik wie nie zuvor.", "it" to "Vivi la musica come mai prima d'ora.",
            "pt" to "Experimente a música como nunca antes.", "ru" to "Испытайте музыку как никогда раньше.",
            "ja" to "これまでにない音楽体験を。", "zh" to "前所未有的音乐体验。", "ar" to "استمتع بالموسيقى كما لم يحدث من قبل.",
            "ko" to "이전과는 다른 음악 경험을 해보세요."
        ),
        "shake_desc" to mapOf(
            "en" to "Shake your phone to shuffle songs.", "tr" to "Şarkıları karıştırmak için telefonu sallayın.",
            "es" to "Agita tu teléfono para mezclar canciones.", "fr" to "Secouez votre téléphone pour mélanger les chansons.",
            "de" to "Schütteln Sie Ihr Telefon, um Lieder zu mischen.", "it" to "Agita il telefono per mescolare le canzoni.",
            "pt" to "Agite seu telefone para embaralhar músicas.", "ru" to "Встряхните телефон, чтобы перемешать песни.",
            "ja" to "電話を振って曲をシャッフルします。", "zh" to "摇动手机以随机播放歌曲。", "ar" to "هز هاتفك لخلط الأغاني.",
            "ko" to "휴대폰을 흔들어 노래를 섞으세요."
        ),
        "earphone_desc" to mapOf(
            "en" to "Auto-play when earphones are plugged in.", "tr" to "Kulaklık takıldığında otomatik çalma.",
            "es" to "Reproducción automática al conectar auriculares.", "fr" to "Lecture automatique lors de la connexion des écouteurs.",
            "de" to "Automatische Wiedergabe beim Anschließen von Kopfhörern.", "it" to "Riproduzione automatica quando si collegano le cuffie.",
            "pt" to "Reprodução automática ao conectar fones de ouvido.", "ru" to "Автовоспроизведение при подключении наушников.",
            "ja" to "イヤホンを接続すると自動再生されます。", "zh" to "插入耳机时自动播放。", "ar" to "التشغيل التلقائي عند توصيل سماعات الأذن.",
            "ko" to "이어폰을 연결하면 자동 재생됩니다."
        ),
        "ready_desc" to mapOf(
            "en" to "You are all set!", "tr" to "Her şey hazır!", "es" to "¡Todo listo!",
            "fr" to "Vous êtes prêt!", "de" to "Sie sind bereit!", "it" to "Sei pronto!",
            "pt" to "Você está pronto!", "ru" to "Вы готовы!", "ja" to "準備完了！",
            "zh" to "一切就绪！", "ar" to "أنت جاهز!", "ko" to "모든 준비가 완료되었습니다!"
        ),
        "ready" to mapOf(
            "en" to "Let's Go!", "tr" to "Başlayalım!", "es" to "¡Vamos!", "fr" to "Allons-y!",
            "de" to "Los geht's!", "it" to "Andiamo!", "pt" to "Vamos lá!", "ru" to "Поехали!",
            "ja" to "始めましょう！", "zh" to "开始吧！", "ar" to "هيا بنا!", "ko" to "시작합시다!"
        ),
        "min_short" to mapOf(
            "en" to "min", "tr" to "dk", "es" to "min", "fr" to "min",
            "de" to "Min", "it" to "min", "pt" to "min", "ru" to "мин",
            "ja" to "分", "zh" to "分", "ar" to "د", "ko" to "분"
        ),
        "sort_date" to mapOf(
            "en" to "Sort by Date", "tr" to "Tarihe Göre", "es" to "Ordenar por fecha", "fr" to "Trier par date",
            "de" to "Nach Datum sortieren", "it" to "Ordina per data", "pt" to "Ordenar por data", "ru" to "Сортировать по дате",
            "ja" to "日付順", "zh" to "按日期排序", "ar" to "ترتيب حسب التاريخ", "ko" to "날짜순 정렬"
        ),
        "sort_name" to mapOf(
            "en" to "Sort by Name", "tr" to "İsme Göre", "es" to "Ordenar por nombre", "fr" to "Trier par nom",
            "de" to "Nach Name sortieren", "it" to "Ordina per nome", "pt" to "Ordenar por nome", "ru" to "Сортировать по имени",
            "ja" to "名前順", "zh" to "按名称排序", "ar" to "ترتيب حسب الاسم", "ko" to "이름순 정렬"
        ),
        "new_playlist" to mapOf(
            "en" to "New Playlist", "tr" to "Yeni Liste", "es" to "Nueva lista", "fr" to "Nouvelle liste",
            "de" to "Neue Wiedergabeliste", "it" to "Nuova playlist", "pt" to "Nova playlist", "ru" to "Новый плейлист",
            "ja" to "新しいプレイリスト", "zh" to "新播放列表", "ar" to "قائمة جديدة", "ko" to "새 재생목록"
        ),
        "playlist_name" to mapOf(
            "en" to "Playlist Name", "tr" to "Liste Adı", "es" to "Nombre de lista", "fr" to "Nom de la liste",
            "de" to "Wiedergabelistenname", "it" to "Nome playlist", "pt" to "Nome da playlist", "ru" to "Название плейлиста",
            "ja" to "プレイリスト名", "zh" to "播放列表名称", "ar" to "اسم القائمة", "ko" to "재생목록 이름"
        ),
        "delete_song" to mapOf(
            "en" to "Delete Song", "tr" to "Şarkıyı Sil", "es" to "Eliminar canción", "fr" to "Supprimer la chanson",
            "de" to "Lied löschen", "it" to "Elimina canzone", "pt" to "Excluir música", "ru" to "Удалить песню",
            "ja" to "曲を削除", "zh" to "删除歌曲", "ar" to "حذف الأغنية", "ko" to "노래 삭제"
        ),
        "add_to_favorites" to mapOf(
            "en" to "Add to Favorites", "tr" to "Favorilere Ekle", "es" to "Añadir a favoritos", "fr" to "Ajouter aux favoris",
            "de" to "Zu Favoriten hinzufügen", "it" to "Aggiungi ai preferiti", "pt" to "Adicionar aos favoritos", "ru" to "Добавить в избранное",
            "ja" to "お気に入りに追加", "zh" to "添加到收藏", "ar" to "إضافة إلى المفضلة", "ko" to "즐겨찾기에 추가"
        ),
        "remove_from_favorites" to mapOf(
            "en" to "Remove from Favorites", "tr" to "Favorilerden Çıkar", "es" to "Quitar de favoritos", "fr" to "Retirer des favoris",
            "de" to "Aus Favoriten entfernen", "it" to "Rimuovi dai preferiti", "pt" to "Remover dos favoritos", "ru" to "Удалить из избранного",
            "ja" to "お気に入りから削除", "zh" to "从收藏中移除", "ar" to "إزالة من المفضلة", "ko" to "즐겨찾기에서 제거"
        ),
        "shake_toast" to mapOf(
            "en" to "Shuffling...", "tr" to "Karıştırılıyor...", "es" to "Mezclando...", "fr" to "Mélange...",
            "de" to "Mischen...", "it" to "Mescolando...", "pt" to "Embaralhando...", "ru" to "Перемешивание...",
            "ja" to "シャッフル中...", "zh" to "随机播放中...", "ar" to "خلط...", "ko" to "섞는 중..."
        ),
        "total_listen" to mapOf(
            "en" to "Total Listening Time", "tr" to "Toplam Dinleme Süresi", "es" to "Tiempo total de escucha",
            "fr" to "Temps d'écoute total", "de" to "Gesamte Hörzeit", "it" to "Tempo di ascolto totale",
            "pt" to "Tempo total de audição", "ru" to "Общее время прослушивания", "ja" to "総リスニング時間",
            "zh" to "总收听时间", "ar" to "إجمالي وقت الاستماع", "ko" to "총 청취 시간"
        ),
        "top_songs" to mapOf(
            "en" to "Top Songs", "tr" to "En Çok Dinlenenler", "es" to "Canciones principales",
            "fr" to "Meilleures chansons", "de" to "Top-Songs", "it" to "Canzoni più ascoltate",
            "pt" to "Músicas principais", "ru" to "Топ песен", "ja" to "トップソング",
            "zh" to "热门歌曲", "ar" to "أفضل الأغاني", "ko" to "인기 곡"
        ),
        "stats" to mapOf(
            "en" to "Statistics", "tr" to "İstatistikler", "es" to "Estadísticas", "fr" to "Statistiques",
            "de" to "Statistiken", "it" to "Statistiche", "pt" to "Estatísticas", "ru" to "Статистика",
            "ja" to "統計", "zh" to "统计", "ar" to "الإحصائيات", "ko" to "통계"
        ),
        "update" to mapOf(
            "en" to "Update", "tr" to "Güncelle", "es" to "Actualizar", "fr" to "Mettre à jour",
            "de" to "Aktualisieren", "it" to "Aggiorna", "pt" to "Atualizar", "ru" to "Обновить",
            "ja" to "更新", "zh" to "更新", "ar" to "تحديث", "ko" to "업데이트"
        ),
        "add_to_playlist" to mapOf(
            "en" to "Add to Playlist", "tr" to "Listeye Ekle", "es" to "Añadir a lista", "fr" to "Ajouter à la liste",
            "de" to "Zur Wiedergabeliste hinzufügen", "it" to "Aggiungi alla playlist", "pt" to "Adicionar à playlist",
            "ru" to "Добавить в плейлист", "ja" to "プレイリストに追加", "zh" to "添加到播放列表",
            "ar" to "إضافة إلى القائمة", "ko" to "재생목록에 추가"
        ),
        "most_played" to mapOf(
            "en" to "Most Played", "tr" to "En Çok Çalınanlar", "es" to "Más reproducidas", "fr" to "Les plus jouées",
            "de" to "Am meisten gespielt", "it" to "Più riprodotte", "pt" to "Mais tocadas", "ru" to "Самые проигрываемые",
            "ja" to "最も再生された", "zh" to "播放最多", "ar" to "الأكثر تشغيلاً", "ko" to "가장 많이 재생됨"
        ),
        "recently_added" to mapOf(
            "en" to "Recently Added", "tr" to "Son Eklenenler", "es" to "Añadidas recientemente", "fr" to "Récemment ajoutées",
            "de" to "Kürzlich hinzugefügt", "it" to "Aggiunte di recente", "pt" to "Adicionadas recentemente",
            "ru" to "Недавно добавленные", "ja" to "最近追加された", "zh" to "最近添加", "ar" to "المضافة مؤخراً",
            "ko" to "최근 추가됨"
        ),
        "all" to mapOf(
            "en" to "All", "tr" to "Hepsi", "es" to "Todos", "fr" to "Tous",
            "de" to "Alle", "it" to "Tutti", "pt" to "Todos", "ru" to "Все",
            "ja" to "すべて", "zh" to "全部", "ar" to "الكل", "ko" to "전체"
        ),
        "delete_confirm" to mapOf(
            "en" to "will be deleted. Do you confirm?", "tr" to "silinecek. Onaylıyor musunuz?",
            "es" to "será eliminado. ¿Confirmas?", "fr" to "sera supprimé. Confirmez-vous?",
            "de" to "wird gelöscht. Bestätigen Sie?", "it" to "sarà eliminato. Confermi?",
            "pt" to "será excluído. Você confirma?", "ru" to "будет удалено. Подтверждаете?",
            "ja" to "削除されます。確認しますか？", "zh" to "将被删除。确认吗？", "ar" to "سيتم حذفه. هل تؤكد؟",
            "ko" to "삭제됩니다. 확인하시겠습니까?"
        ),
        "user_agreement" to mapOf(
            "en" to "User Agreement", "tr" to "Kullanıcı Sözleşmesi", "es" to "Acuerdo de usuario",
            "fr" to "Accord d'utilisateur", "de" to "Benutzervereinbarung", "it" to "Accordo utente",
            "pt" to "Acordo do usuário", "ru" to "Пользовательское соглашение", "ja" to "ユーザー契約",
            "zh" to "用户协议", "ar" to "اتفاقية المستخدم", "ko" to "사용자 계약"
        ),
        "user_agreement_short" to mapOf(
            "en" to "By using BeatBox, you agree to our terms. We respect your privacy - no data is collected or shared. All music stays on your device.",
            "tr" to "BeatBox'ı kullanarak şartlarımızı kabul etmiş olursunuz. Gizliliğinize saygı duyuyoruz - hiçbir veri toplanmaz veya paylaşılmaz. Tüm müzikler cihazınızda kalır.",
            "es" to "Al usar BeatBox, aceptas nuestros términos. Respetamos tu privacidad: no se recopilan ni comparten datos. Toda la música permanece en tu dispositivo.",
            "fr" to "En utilisant BeatBox, vous acceptez nos conditions. Nous respectons votre vie privée - aucune donnée n'est collectée ou partagée. Toute la musique reste sur votre appareil.",
            "de" to "Durch die Nutzung von BeatBox stimmen Sie unseren Bedingungen zu. Wir respektieren Ihre Privatsphäre - es werden keine Daten gesammelt oder geteilt. Alle Musik bleibt auf Ihrem Gerät.",
            "it" to "Utilizzando BeatBox, accetti i nostri termini. Rispettiamo la tua privacy - nessun dato viene raccolto o condiviso. Tutta la musica rimane sul tuo dispositivo.",
            "pt" to "Ao usar o BeatBox, você concorda com nossos termos. Respeitamos sua privacidade - nenhum dado é coletado ou compartilhado. Toda a música permanece no seu dispositivo.",
            "ru" to "Используя BeatBox, вы соглашаетесь с нашими условиями. Мы уважаем вашу конфиденциальность - никакие данные не собираются и не передаются. Вся музыка остается на вашем устройстве.",
            "ja" to "BeatBoxを使用することで、当社の利用規約に同意したことになります。プライバシーを尊重します - データは収集または共有されません。すべての音楽はデバイスに残ります。",
            "zh" to "使用BeatBox即表示您同意我们的条款。我们尊重您的隐私 - 不收集或共享任何数据。所有音乐都保留在您的设备上。",
            "ar" to "باستخدام BeatBox، فإنك توافق على شروطنا. نحن نحترم خصوصيتك - لا يتم جمع أو مشاركة أي بيانات. تبقى جميع الموسيقى على جهازك.",
            "ko" to "BeatBox를 사용함으로써 당사의 약관에 동의하는 것입니다. 귀하의 개인정보를 존중합니다 - 데이터가 수집되거나 공유되지 않습니다. 모든 음악은 기기에 남아 있습니다."
        ),
        "user_agreement_full" to mapOf(
            "en" to "BeatBox User Agreement\n\nLast Updated: 2025\n\n1. General Terms\nBy using the BeatBox application, you agree to this agreement. The app is designed to play music files on your device.\n\n2. Privacy and Data Usage\n• BeatBox requires permission to access music files on your device.\n• No personal data or music files are sent to our servers.\n• All data is stored locally on your device.\n• No internet connection required.\n\n3. Permissions\n• Music and Audio Files: Required to access your music library.\n• Microphone (Visualizer): Used only for audio visualization, no recording.\n• Notifications: Required for music control.\n\n4. Copyright\n• Users must be the legal owner of music files on their devices.\n• BeatBox is not responsible for copyright violations.\n• Only listen to music you legally own.\n\n5. Disclaimer\n• The app is provided \"as is\".\n• We are not responsible for data loss or device damage.\n• You use the app at your own risk.\n\n6. Changes\nThis agreement may be updated at any time. Updates will be announced within the app.\n\n7. Contact\nFor questions: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Developed by BlackStack Labs.",
            "tr" to "BeatBox Kullanıcı Sözleşmesi\n\nSon Güncelleme: 2025\n\n1. Genel Koşullar\nBeatBox uygulamasını kullanarak bu sözleşmeyi kabul etmiş sayılırsınız. Uygulama, cihazınızdaki müzik dosyalarını oynatmak için tasarlanmıştır.\n\n2. Gizlilik ve Veri Kullanımı\n• BeatBox, cihazınızdaki müzik dosyalarına erişim izni gerektirir.\n• Hiçbir kişisel veri veya müzik dosyası sunucularımıza gönderilmez.\n• Tüm veriler cihazınızda yerel olarak saklanır.\n• İnternet bağlantısı gerektirmez.\n\n3. İzinler\n• Müzik ve Ses Dosyaları: Müzik kütüphanenize erişim için gereklidir.\n• Mikrofon (Visualizer): Sadece ses görselleştirme için kullanılır, kayıt yapılmaz.\n• Bildirimler: Müzik kontrolü için gereklidir.\n\n4. Telif Hakları\n• Kullanıcılar, cihazlarındaki müzik dosyalarının yasal sahibi olmalıdır.\n• BeatBox, telif hakkı ihlallerinden sorumlu değildir.\n• Sadece yasal olarak sahip olduğunuz müzikleri dinleyin.\n\n5. Sorumluluk Reddi\n• Uygulama \"olduğu gibi\" sunulmaktadır.\n• Veri kaybı veya cihaz hasarından sorumlu değiliz.\n• Uygulamayı kendi sorumluluğunuzda kullanırsınız.\n\n6. Değişiklikler\nBu sözleşme herhangi bir zamanda güncellenebilir. Güncellemeler uygulama içinde duyurulacaktır.\n\n7. İletişim\nSorularınız için: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - BlackStack Labs tarafından geliştirilmiştir.",
            "es" to "Acuerdo de Usuario de BeatBox\n\nÚltima actualización: 2025\n\n1. Términos Generales\nAl usar la aplicación BeatBox, aceptas este acuerdo. La aplicación está diseñada para reproducir archivos de música en tu dispositivo.\n\n2. Privacidad y Uso de Datos\n• BeatBox requiere permiso para acceder a archivos de música en tu dispositivo.\n• No se envían datos personales ni archivos de música a nuestros servidores.\n• Todos los datos se almacenan localmente en tu dispositivo.\n• No se requiere conexión a Internet.\n\n3. Permisos\n• Archivos de Música y Audio: Requerido para acceder a tu biblioteca de música.\n• Micrófono (Visualizador): Usado solo para visualización de audio, sin grabación.\n• Notificaciones: Requerido para control de música.\n\n4. Derechos de Autor\n• Los usuarios deben ser propietarios legales de los archivos de música en sus dispositivos.\n• BeatBox no es responsable de violaciones de derechos de autor.\n• Solo escucha música que poseas legalmente.\n\n5. Descargo de Responsabilidad\n• La aplicación se proporciona \"tal cual\".\n• No somos responsables de pérdida de datos o daños al dispositivo.\n• Usas la aplicación bajo tu propio riesgo.\n\n6. Cambios\nEste acuerdo puede actualizarse en cualquier momento. Las actualizaciones se anunciarán dentro de la aplicación.\n\n7. Contacto\nPara preguntas: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Desarrollado por BlackStack Labs.",
            "fr" to "Accord d'Utilisateur BeatBox\n\nDernière mise à jour: 2025\n\n1. Conditions Générales\nEn utilisant l'application BeatBox, vous acceptez cet accord. L'application est conçue pour lire des fichiers musicaux sur votre appareil.\n\n2. Confidentialité et Utilisation des Données\n• BeatBox nécessite l'autorisation d'accéder aux fichiers musicaux sur votre appareil.\n• Aucune donnée personnelle ou fichier musical n'est envoyé à nos serveurs.\n• Toutes les données sont stockées localement sur votre appareil.\n• Aucune connexion Internet requise.\n\n3. Autorisations\n• Fichiers Musicaux et Audio: Requis pour accéder à votre bibliothèque musicale.\n• Microphone (Visualiseur): Utilisé uniquement pour la visualisation audio, pas d'enregistrement.\n• Notifications: Requis pour le contrôle de la musique.\n\n4. Droits d'Auteur\n• Les utilisateurs doivent être propriétaires légaux des fichiers musicaux sur leurs appareils.\n• BeatBox n'est pas responsable des violations de droits d'auteur.\n• N'écoutez que la musique que vous possédez légalement.\n\n5. Clause de Non-Responsabilité\n• L'application est fournie \"telle quelle\".\n• Nous ne sommes pas responsables de la perte de données ou des dommages à l'appareil.\n• Vous utilisez l'application à vos propres risques.\n\n6. Modifications\nCet accord peut être mis à jour à tout moment. Les mises à jour seront annoncées dans l'application.\n\n7. Contact\nPour les questions: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Développé par BlackStack Labs.",
            "de" to "BeatBox Benutzervereinbarung\n\nLetzte Aktualisierung: 2025\n\n1. Allgemeine Bedingungen\nDurch die Nutzung der BeatBox-Anwendung stimmen Sie dieser Vereinbarung zu. Die App ist zum Abspielen von Musikdateien auf Ihrem Gerät konzipiert.\n\n2. Datenschutz und Datennutzung\n• BeatBox benötigt die Berechtigung zum Zugriff auf Musikdateien auf Ihrem Gerät.\n• Keine persönlichen Daten oder Musikdateien werden an unsere Server gesendet.\n• Alle Daten werden lokal auf Ihrem Gerät gespeichert.\n• Keine Internetverbindung erforderlich.\n\n3. Berechtigungen\n• Musik- und Audiodateien: Erforderlich für den Zugriff auf Ihre Musikbibliothek.\n• Mikrofon (Visualizer): Nur für Audio-Visualisierung verwendet, keine Aufnahme.\n• Benachrichtigungen: Erforderlich für Musiksteuerung.\n\n4. Urheberrecht\n• Benutzer müssen rechtmäßige Eigentümer der Musikdateien auf ihren Geräten sein.\n• BeatBox ist nicht verantwortlich für Urheberrechtsverletzungen.\n• Hören Sie nur Musik, die Sie rechtmäßig besitzen.\n\n5. Haftungsausschluss\n• Die App wird \"wie besehen\" bereitgestellt.\n• Wir sind nicht verantwortlich für Datenverlust oder Geräteschäden.\n• Sie verwenden die App auf eigenes Risiko.\n\n6. Änderungen\nDiese Vereinbarung kann jederzeit aktualisiert werden. Aktualisierungen werden in der App angekündigt.\n\n7. Kontakt\nFür Fragen: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Entwickelt von BlackStack Labs.",
            "it" to "Accordo Utente BeatBox\n\nUltimo aggiornamento: 2025\n\n1. Termini Generali\nUtilizzando l'applicazione BeatBox, accetti questo accordo. L'app è progettata per riprodurre file musicali sul tuo dispositivo.\n\n2. Privacy e Utilizzo dei Dati\n• BeatBox richiede l'autorizzazione per accedere ai file musicali sul tuo dispositivo.\n• Nessun dato personale o file musicale viene inviato ai nostri server.\n• Tutti i dati sono archiviati localmente sul tuo dispositivo.\n• Nessuna connessione Internet richiesta.\n\n3. Autorizzazioni\n• File Musicali e Audio: Richiesto per accedere alla tua libreria musicale.\n• Microfono (Visualizzatore): Utilizzato solo per la visualizzazione audio, nessuna registrazione.\n• Notifiche: Richiesto per il controllo della musica.\n\n4. Copyright\n• Gli utenti devono essere proprietari legali dei file musicali sui loro dispositivi.\n• BeatBox non è responsabile per violazioni del copyright.\n• Ascolta solo musica che possiedi legalmente.\n\n5. Esclusione di Responsabilità\n• L'app è fornita \"così com'è\".\n• Non siamo responsabili per perdita di dati o danni al dispositivo.\n• Utilizzi l'app a tuo rischio.\n\n6. Modifiche\nQuesto accordo può essere aggiornato in qualsiasi momento. Gli aggiornamenti saranno annunciati nell'app.\n\n7. Contatto\nPer domande: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Sviluppato da BlackStack Labs.",
            "pt" to "Acordo do Usuário BeatBox\n\nÚltima atualização: 2025\n\n1. Termos Gerais\nAo usar o aplicativo BeatBox, você concorda com este acordo. O aplicativo foi projetado para reproduzir arquivos de música em seu dispositivo.\n\n2. Privacidade e Uso de Dados\n• BeatBox requer permissão para acessar arquivos de música em seu dispositivo.\n• Nenhum dado pessoal ou arquivo de música é enviado para nossos servidores.\n• Todos os dados são armazenados localmente em seu dispositivo.\n• Nenhuma conexão com a Internet necessária.\n\n3. Permissões\n• Arquivos de Música e Áudio: Necessário para acessar sua biblioteca de música.\n• Microfone (Visualizador): Usado apenas para visualização de áudio, sem gravação.\n• Notificações: Necessário para controle de música.\n\n4. Direitos Autorais\n• Os usuários devem ser proprietários legais dos arquivos de música em seus dispositivos.\n• BeatBox não é responsável por violações de direitos autorais.\n• Ouça apenas música que você possui legalmente.\n\n5. Isenção de Responsabilidade\n• O aplicativo é fornecido \"como está\".\n• Não somos responsáveis por perda de dados ou danos ao dispositivo.\n• Você usa o aplicativo por sua própria conta e risco.\n\n6. Alterações\nEste acordo pode ser atualizado a qualquer momento. As atualizações serão anunciadas no aplicativo.\n\n7. Contato\nPara perguntas: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Desenvolvido por BlackStack Labs.",
            "ru" to "Пользовательское соглашение BeatBox\n\nПоследнее обновление: 2025\n\n1. Общие условия\nИспользуя приложение BeatBox, вы соглашаетесь с этим соглашением. Приложение предназначено для воспроизведения музыкальных файлов на вашем устройстве.\n\n2. Конфиденциальность и использование данных\n• BeatBox требует разрешения на доступ к музыкальным файлам на вашем устройстве.\n• Никакие личные данные или музыкальные файлы не отправляются на наши серверы.\n• Все данные хранятся локально на вашем устройстве.\n• Подключение к Интернету не требуется.\n\n3. Разрешения\n• Музыкальные и аудиофайлы: Требуется для доступа к вашей музыкальной библиотеке.\n• Микрофон (Визуализатор): Используется только для визуализации звука, без записи.\n• Уведомления: Требуется для управления музыкой.\n\n4. Авторские права\n• Пользователи должны быть законными владельцами музыкальных файлов на своих устройствах.\n• BeatBox не несет ответственности за нарушения авторских прав.\n• Слушайте только музыку, которой вы законно владеете.\n\n5. Отказ от ответственности\n• Приложение предоставляется \"как есть\".\n• Мы не несем ответственности за потерю данных или повреждение устройства.\n• Вы используете приложение на свой страх и риск.\n\n6. Изменения\nЭто соглашение может быть обновлено в любое время. Обновления будут объявлены в приложении.\n\n7. Контакт\nПо вопросам: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - Разработано BlackStack Labs.",
            "ja" to "BeatBoxユーザー契約\n\n最終更新: 2025\n\n1. 一般条件\nBeatBoxアプリケーションを使用することにより、本契約に同意したことになります。このアプリは、デバイス上の音楽ファイルを再生するために設計されています。\n\n2. プライバシーとデータ使用\n• BeatBoxは、デバイス上の音楽ファイルへのアクセス許可が必要です。\n• 個人データや音楽ファイルは当社のサーバーに送信されません。\n• すべてのデータはデバイスにローカルに保存されます。\n• インターネット接続は不要です。\n\n3. 許可\n• 音楽およびオーディオファイル: 音楽ライブラリへのアクセスに必要です。\n• マイク（ビジュアライザー）: オーディオの視覚化にのみ使用され、録音はされません。\n• 通知: 音楽コントロールに必要です。\n\n4. 著作権\n• ユーザーは、デバイス上の音楽ファイルの合法的な所有者でなければなりません。\n• BeatBoxは著作権侵害について責任を負いません。\n• 合法的に所有している音楽のみを聴いてください。\n\n5. 免責事項\n• アプリは「現状のまま」提供されます。\n• データの損失やデバイスの損傷について責任を負いません。\n• アプリの使用は自己責任で行ってください。\n\n6. 変更\n本契約はいつでも更新される可能性があります。更新はアプリ内で発表されます。\n\n7. お問い合わせ\nご質問は: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - BlackStack Labsによって開発されました。",
            "zh" to "BeatBox用户协议\n\n最后更新: 2025\n\n1. 一般条款\n使用BeatBox应用程序即表示您同意本协议。该应用程序旨在播放您设备上的音乐文件。\n\n2. 隐私和数据使用\n• BeatBox需要访问您设备上的音乐文件的权限。\n• 不会将任何个人数据或音乐文件发送到我们的服务器。\n• 所有数据都本地存储在您的设备上。\n• 不需要互联网连接。\n\n3. 权限\n• 音乐和音频文件: 访问您的音乐库所需。\n• 麦克风（可视化器）: 仅用于音频可视化，不进行录音。\n• 通知: 音乐控制所需。\n\n4. 版权\n• 用户必须是其设备上音乐文件的合法所有者。\n• BeatBox不对版权侵权负责。\n• 只听您合法拥有的音乐。\n\n5. 免责声明\n• 该应用程序按\"原样\"提供。\n• 我们不对数据丢失或设备损坏负责。\n• 您使用该应用程序的风险自负。\n\n6. 更改\n本协议可能随时更新。更新将在应用程序内公布。\n\n7. 联系方式\n如有问题: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - 由BlackStack Labs开发。",
            "ar" to "اتفاقية مستخدم BeatBox\n\nآخر تحديث: 2025\n\n1. الشروط العامة\nباستخدام تطبيق BeatBox، فإنك توافق على هذه الاتفاقية. التطبيق مصمم لتشغيل ملفات الموسيقى على جهازك.\n\n2. الخصوصية واستخدام البيانات\n• يتطلب BeatBox إذنًا للوصول إلى ملفات الموسيقى على جهازك.\n• لا يتم إرسال أي بيانات شخصية أو ملفات موسيقى إلى خوادمنا.\n• يتم تخزين جميع البيانات محليًا على جهازك.\n• لا يلزم الاتصال بالإنترنت.\n\n3. الأذونات\n• ملفات الموسيقى والصوت: مطلوب للوصول إلى مكتبة الموسيقى الخاصة بك.\n• الميكروفون (المصور): يستخدم فقط لتصور الصوت، بدون تسجيل.\n• الإشعارات: مطلوب للتحكم في الموسيقى.\n\n4. حقوق النشر\n• يجب أن يكون المستخدمون المالكين القانونيين لملفات الموسيقى على أجهزتهم.\n• BeatBox غير مسؤول عن انتهاكات حقوق النشر.\n• استمع فقط إلى الموسيقى التي تمتلكها بشكل قانوني.\n\n5. إخلاء المسؤولية\n• يتم توفير التطبيق \"كما هو\".\n• نحن غير مسؤولين عن فقدان البيانات أو تلف الجهاز.\n• أنت تستخدم التطبيق على مسؤوليتك الخاصة.\n\n6. التغييرات\nيمكن تحديث هذه الاتفاقية في أي وقت. سيتم الإعلان عن التحديثات داخل التطبيق.\n\n7. الاتصال\nللأسئلة: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - تم التطوير بواسطة BlackStack Labs.",
            "ko" to "BeatBox 사용자 계약\n\n마지막 업데이트: 2025\n\n1. 일반 조건\nBeatBox 애플리케이션을 사용함으로써 본 계약에 동의하는 것입니다. 이 앱은 기기의 음악 파일을 재생하도록 설계되었습니다.\n\n2. 개인정보 보호 및 데이터 사용\n• BeatBox는 기기의 음악 파일에 액세스할 수 있는 권한이 필요합니다.\n• 개인 데이터나 음악 파일은 당사 서버로 전송되지 않습니다.\n• 모든 데이터는 기기에 로컬로 저장됩니다.\n• 인터넷 연결이 필요하지 않습니다.\n\n3. 권한\n• 음악 및 오디오 파일: 음악 라이브러리에 액세스하는 데 필요합니다.\n• 마이크(시각화 도구): 오디오 시각화에만 사용되며 녹음되지 않습니다.\n• 알림: 음악 제어에 필요합니다.\n\n4. 저작권\n• 사용자는 기기의 음악 파일의 합법적인 소유자여야 합니다.\n• BeatBox는 저작권 침해에 대해 책임지지 않습니다.\n• 합법적으로 소유한 음악만 들으십시오.\n\n5. 면책 조항\n• 앱은 \"있는 그대로\" 제공됩니다.\n• 데이터 손실이나 기기 손상에 대해 책임지지 않습니다.\n• 앱 사용은 본인의 책임입니다.\n\n6. 변경 사항\n본 계약은 언제든지 업데이트될 수 있습니다. 업데이트는 앱 내에서 공지됩니다.\n\n7. 연락처\n문의 사항: dev.blackstackslabs@gmail.com\n\nBeatBox v3.0 - BlackStack Labs에서 개발했습니다."
        ),
        "read_full_agreement" to mapOf(
            "en" to "Read Full Agreement", "tr" to "Tam Sözleşmeyi Oku", "es" to "Leer acuerdo completo",
            "fr" to "Lire l'accord complet", "de" to "Vollständige Vereinbarung lesen", "it" to "Leggi accordo completo",
            "pt" to "Ler acordo completo", "ru" to "Прочитать полное соглашение", "ja" to "完全な契約を読む",
            "zh" to "阅读完整协议", "ar" to "قراءة الاتفاقية الكاملة", "ko" to "전체 계약 읽기"
        ),
        "about_desc" to mapOf(
            "en" to "BeatBox v3.0\nDeveloped by BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "tr" to "BeatBox v3.0\nBlackStack Labs tarafından geliştirilmiştir\n\nGörüşleriniz için: dev.blackstackslabs@gmail.com",
            "es" to "BeatBox v3.0\nDesarrollado por BlackStack Labs\n\nComentarios: dev.blackstackslabs@gmail.com",
            "fr" to "BeatBox v3.0\nDéveloppé par BlackStack Labs\n\nRetour: dev.blackstackslabs@gmail.com",
            "de" to "BeatBox v3.0\nEntwickelt von BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "it" to "BeatBox v3.0\nSviluppato da BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "pt" to "BeatBox v3.0\nDesenvolvido por BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "ru" to "BeatBox v3.0\nРазработано BlackStack Labs\n\nОбратная связь: dev.blackstackslabs@gmail.com",
            "ja" to "BeatBox v3.0\nBlackStack Labsによって開発\n\nフィードバック: dev.blackstackslabs@gmail.com",
            "zh" to "BeatBox v3.0\n由BlackStack Labs开发\n\n反馈: dev.blackstackslabs@gmail.com",
            "ar" to "BeatBox v3.0\nتم التطوير بواسطة BlackStack Labs\n\nالملاحظات: dev.blackstackslabs@gmail.com",
            "ko" to "BeatBox v3.0\nBlackStack Labs에서 개발\n\n피드백: dev.blackstackslabs@gmail.com"
        ),
        "earphone_intelligence" to mapOf(
            "en" to "Earphone Intelligence", "tr" to "Kulaklık Zekası", "es" to "Inteligencia de auriculares",
            "fr" to "Intelligence des écouteurs", "de" to "Kopfhörer-Intelligenz", "it" to "Intelligenza cuffie",
            "pt" to "Inteligência de fones", "ru" to "Интеллект наушников", "ja" to "イヤホンインテリジェンス",
            "zh" to "耳机智能", "ar" to "ذكاء سماعات الأذن", "ko" to "이어폰 인텔리전스"
        ),
        "gradient" to mapOf(
            "en" to "Gradient", "tr" to "Geçişli", "es" to "Degradado", "fr" to "Dégradé",
            "de" to "Verlauf", "it" to "Sfumatura", "pt" to "Gradiente", "ru" to "Градиент",
            "ja" to "グラデーション", "zh" to "渐变", "ar" to "تدرج", "ko" to "그라데이션"
        ),
        "skip" to mapOf(
            "en" to "Skip", "tr" to "Atla", "es" to "Saltar", "fr" to "Passer",
            "de" to "Überspringen", "it" to "Salta", "pt" to "Pular", "ru" to "Пропустить",
            "ja" to "スキップ", "zh" to "跳过", "ar" to "تخطي", "ko" to "건너뛰기"
        ),
        "next" to mapOf(
            "en" to "Next", "tr" to "İleri", "es" to "Siguiente", "fr" to "Suivant",
            "de" to "Weiter", "it" to "Avanti", "pt" to "Próximo", "ru" to "Далее",
            "ja" to "次へ", "zh" to "下一步", "ar" to "التالي", "ko" to "다음"
        ),
        "default" to mapOf(
            "en" to "Default", "tr" to "Varsayılan", "es" to "Predeterminado", "fr" to "Par défaut",
            "de" to "Standard", "it" to "Predefinito", "pt" to "Padrão", "ru" to "По умолчанию",
            "ja" to "デフォルト", "zh" to "默认", "ar" to "افتراضي", "ko" to "기본값"
        ),
        "system_default" to mapOf(
            "en" to "System Default", "tr" to "Sistem Varsayılanı", "es" to "Predeterminado del sistema", "fr" to "Par défaut du système",
            "de" to "Systemstandard", "it" to "Predefinito di sistema", "pt" to "Padrão do sistema", "ru" to "Системный по умолчанию",
            "ja" to "システムデフォルト", "zh" to "系统默认", "ar" to "النظام الافتراضي", "ko" to "시스템 기본값"
        ),
        "lyrics" to mapOf(
            "en" to "Lyrics", "tr" to "Şarkı Sözleri", "es" to "Letras", "fr" to "Paroles",
            "de" to "Texte", "it" to "Testi", "pt" to "Letras", "ru" to "Текст песни",
            "ja" to "歌詞", "zh" to "歌词", "ar" to "كلمات", "ko" to "가사"
        ),
        "no_lyrics_found" to mapOf(
            "en" to "No lyrics found", "tr" to "Şarkı sözü bulunamadı", "es" to "No se encontraron letras",
            "fr" to "Aucune parole trouvée", "de" to "Keine Texte gefunden", "it" to "Nessun testo trovato",
            "pt" to "Nenhuma letra encontrada", "ru" to "Текст не найден", "ja" to "歌詞が見つかりません",
            "zh" to "未找到歌词", "ar" to "لم يتم العثور على كلمات", "ko" to "가사를 찾을 수 없습니다"
        ),
        "loading_lyrics" to mapOf(
            "en" to "Loading lyrics...", "tr" to "Şarkı sözleri yükleniyor...", "es" to "Cargando letras...",
            "fr" to "Chargement des paroles...", "de" to "Texte werden geladen...", "it" to "Caricamento testi...",
            "pt" to "Carregando letras...", "ru" to "Загрузка текста...", "ja" to "歌詞を読み込んでいます...",
            "zh" to "加载歌词中...", "ar" to "تحميل الكلمات...", "ko" to "가사 로딩 중..."
        ),
        "lyrics_error" to mapOf(
            "en" to "Lyrics error", "tr" to "Şarkı sözü hatası", "es" to "Error de letras",
            "fr" to "Erreur de paroles", "de" to "Textfehler", "it" to "Errore testi",
            "pt" to "Erro de letras", "ru" to "Ошибка текста", "ja" to "歌詞エラー",
            "zh" to "歌词错误", "ar" to "خطأ في الكلمات", "ko" to "가사 오류"
        ),
        "retry" to mapOf(
            "en" to "Retry", "tr" to "Tekrar Dene", "es" to "Reintentar", "fr" to "Réessayer",
            "de" to "Wiederholen", "it" to "Riprova", "pt" to "Tentar novamente", "ru" to "Повторить",
            "ja" to "再試行", "zh" to "重试", "ar" to "إعادة المحاولة", "ko" to "재시도"
        ),
        "audio_output_device" to mapOf(
            "en" to "Audio Output Device", "tr" to "Ses Çıkış Cihazı", "es" to "Dispositivo de salida de audio",
            "fr" to "Périphérique de sortie audio", "de" to "Audio-Ausgabegerät", "it" to "Dispositivo di uscita audio",
            "pt" to "Dispositivo de saída de áudio", "ru" to "Устройство вывода звука", "ja" to "オーディオ出力デバイス",
            "zh" to "音频输出设备", "ar" to "جهاز إخراج الصوت", "ko" to "오디오 출력 장치"
        ),
        "bluetooth" to mapOf(
            "en" to "Bluetooth", "tr" to "Bluetooth", "es" to "Bluetooth", "fr" to "Bluetooth",
            "de" to "Bluetooth", "it" to "Bluetooth", "pt" to "Bluetooth", "ru" to "Bluetooth",
            "ja" to "Bluetooth", "zh" to "蓝牙", "ar" to "بلوتوث", "ko" to "블루투스"
        ),
        "headphones" to mapOf(
            "en" to "Headphones", "tr" to "Kulaklık", "es" to "Auriculares", "fr" to "Écouteurs",
            "de" to "Kopfhörer", "it" to "Cuffie", "pt" to "Fones de ouvido", "ru" to "Наушники",
            "ja" to "ヘッドフォン", "zh" to "耳机", "ar" to "سماعات الرأس", "ko" to "헤드폰"
        ),
        "phone_speaker" to mapOf(
            "en" to "Phone Speaker", "tr" to "Telefon Hoparlörü", "es" to "Altavoz del teléfono", "fr" to "Haut-parleur du téléphone",
            "de" to "Telefon-Lautsprecher", "it" to "Altoparlante del telefono", "pt" to "Alto-falante do telefone", "ru" to "Динамик телефона",
            "ja" to "電話スピーカー", "zh" to "手机扬声器", "ar" to "مكبر صوت الهاتف", "ko" to "휴대폰 스피커"
        ),
        "switched_to" to mapOf(
            "en" to "Switched to", "tr" to "Geçildi:", "es" to "Cambiado a", "fr" to "Basculé vers",
            "de" to "Gewechselt zu", "it" to "Passato a", "pt" to "Mudado para", "ru" to "Переключено на",
            "ja" to "切り替え:", "zh" to "切换到", "ar" to "تم التبديل إلى", "ko" to "전환됨:"
        ),
        "failed_to_switch_device" to mapOf(
            "en" to "Failed to switch device", "tr" to "Cihaz değiştirilemedi", "es" to "Error al cambiar dispositivo",
            "fr" to "Échec du changement de périphérique", "de" to "Gerätewechsel fehlgeschlagen", "it" to "Impossibile cambiare dispositivo",
            "pt" to "Falha ao mudar dispositivo", "ru" to "Не удалось переключить устройство", "ja" to "デバイスの切り替えに失敗しました",
            "zh" to "切换设备失败", "ar" to "فشل تبديل الجهاز", "ko" to "장치 전환 실패"
        ),
        "onboarding_title_1" to mapOf(
            "en" to "Welcome to BeatBox", "tr" to "BeatBox'a Hoş Geldiniz", "es" to "Bienvenido a BeatBox",
            "fr" to "Bienvenue sur BeatBox", "de" to "Willkommen bei BeatBox", "it" to "Benvenuto in BeatBox",
            "pt" to "Bem-vindo ao BeatBox", "ru" to "Добро пожаловать в BeatBox", "ja" to "BeatBoxへようこそ",
            "zh" to "欢迎使用 BeatBox", "ar" to "مرحباً بك في BeatBox", "ko" to "BeatBox에 오신 것을 환영합니다"
        ),
        "onboarding_desc_1" to mapOf(
            "en" to "Listen to your music in the highest quality. Feel the beat with an advanced equalizer and audio visualizer.",
            "tr" to "Müziğinizi en yüksek kalitede dinleyin, gelişmiş ekolayzer ve ritim görselleştirici ile ritmi hissedin.",
            "es" to "Escucha tu música con la máxima calidad. Siente el ritmo con ecualizador avanzado y visualizador de audio.",
            "fr" to "Écoutez votre musique en haute qualité. Ressentez le rythme avec un égaliseur avancé et un visualiseur audio.",
            "de" to "Höre deine Musik in höchster Qualität. Spüre den Beat mit modernem Equalizer und Audio-Visualisierer.",
            "it" to "Ascolta la tua musica alla massima qualità. Senti il ritmo con equalizzatore avanzato e visualizzatore audio.",
            "pt" to "Ouça suas músicas com a mais alta qualidade. Sinta a batida com equalizador avançado e visualizador de áudio.",
            "ru" to "Слушайте музыку в высочайшем качестве. Почувствуйте ритм с эквалайзером и визуализатором звука.",
            "ja" to "最高音質で音楽を聴きましょう。高度なイコライザーとビジュアライザーでビートを感じてください。",
            "zh" to "以最高品质聆听音乐，借助高级均衡器和节奏可视化器感受音乐节拍。",
            "ar" to "استمع إلى موسيقاك بأعلى جودة. اشعر بالإيقاع مع معادل متقدم ومؤثرات بصرية صوتية.",
            "ko" to "최고 음질로 음악을 감상하세요. 고급 이퀄라이저와 비주얼라이저로 비트를 느껴보세요."
        ),
        "onboarding_title_2" to mapOf(
            "en" to "Synchronized Lyrics", "tr" to "Senkronize Şarkı Sözleri", "es" to "Letras sincronizadas",
            "fr" to "Paroles synchronisées", "de" to "Synchrone Songtexte", "it" to "Testi sincronizzati",
            "pt" to "Letras sincronizadas", "ru" to "Синхронизированные тексты", "ja" to "同期歌詞",
            "zh" to "同步歌词", "ar" to "كلمات متزامنة", "ko" to "동기화된 가사"
        ),
        "onboarding_desc_2" to mapOf(
            "en" to "Follow song lyrics in real-time with automatic synchronization and sing along to every beat.",
            "tr" to "Dinlediğiniz şarkıların sözlerini anlık olarak eşzamanlı takip edin, her ritme eşlik edin.",
            "es" to "Sigue las letras de las canciones en tiempo real con sincronización automática y canta con cada ritmo.",
            "fr" to "Suivez les paroles de chansons en temps réel avec synchronisation automatique et chantez à chaque rythme.",
            "de" to "Verfolge Songtexte in Echtzeit mit automatischer Synchronisation und singe zu jedem Beat mit.",
            "it" to "Segui i testi delle canzoni in tempo reale con sincronizzazione automatica e canta ad ogni ritmo.",
            "pt" to "Acompanhe as letras das músicas em tempo real com sincronização automática e cante junto.",
            "ru" to "Следите за текстом песен в реальном времени с синхронизацией и подпевайте каждому биту.",
            "ja" to "自動同期でリアルタイムに歌詞を追い、あらゆるビートに合わせて歌いましょう。",
            "zh" to "实时同步歌词，随着每个节拍尽情歌唱。",
            "ar" to "تابع كلمات الأغاني في الوقت الفعلي مع المزامنة التلقائية وغنِّ مع كل إيقاع.",
            "ko" to "실시간 자동 동기화 가사를 따라가며 모든 비트에 맞춰 노래하세요."
        ),
        "onboarding_title_3" to mapOf(
            "en" to "15+ Stunning Themes", "tr" to "15+ Büyüleyici Tema", "es" to "Más de 15 temas increíbles",
            "fr" to "15+ thèmes superbes", "de" to "15+ beeindruckende Themes", "it" to "15+ fantastici temi",
            "pt" to "15+ temas incríveis", "ru" to "15+ потрясающих тем", "ja" to "15種類以上の美しいテーマ",
            "zh" to "15+ 炫酷主题", "ar" to "أكثر من 15 سمة مذهلة", "ko" to "15개 이상의 멋진 테마"
        ),
        "onboarding_desc_3" to mapOf(
            "en" to "Personalize the player with Cyberpunk, Pitch Black, Rose Gold and many more unique themes.",
            "tr" to "Siberpunk, Zifiri Siyah, Roz Altın ve daha birçok özel temayla oyuncu görünümünü kişiselleştirin.",
            "es" to "Personaliza el reproductor con Cyberpunk, Negro Puro, Oro Rosa y muchos más temas únicos.",
            "fr" to "Personnalisez le lecteur avec Cyberpunk, Noir Absolu, Or Rose et bien d'autres thèmes uniques.",
            "de" to "Personalisiere den Player mit Cyberpunk, Tiefschwarz, Roségold und vielen weiteren Themes.",
            "it" to "Personalizza il lettore con Cyberpunk, Nero Assoluto, Oro Rosa e molti altri temi unici.",
            "pt" to "Personalize o player com Cyberpunk, Preto Absoluto, Ouro Rosa e muitos outros temas exclusivos.",
            "ru" to "Настройте плеер с помощью тем: Киберпанк, Глубокий черный, Розовое золото и многих других.",
            "ja" to "サイバーパンク、ピュアブラック、ローズゴールドなど多彩なテーマでプレーヤーをカスタマイズ。",
            "zh" to "使用赛博朋克、纯黑、玫瑰金等多种独特主题个性化播放器。",
            "ar" to "قم بتخصيص المشغل مع سمات سايبربانك، الأسود الداكن، الذهب الوردي والعديد من السمات الفريدة.",
            "ko" to "사이버펑크, 올레드 블랙, 로즈 골드 등 다양한 고유 테마로 플레이어를 맞춤 설정하세요."
        ),
        "onboarding_title_4" to mapOf(
            "en" to "Permissions & Access", "tr" to "İzinler ve Erişim", "es" to "Permisos y acceso",
            "fr" to "Autorisations et accès", "de" to "Berechtigungen und Zugriff", "it" to "Permessi e accesso",
            "pt" to "Permissões e acesso", "ru" to "Разрешения и доступ", "ja" to "権限とアクセス",
            "zh" to "权限与访问", "ar" to "الأذونات والوصول", "ko" to "권한 및 접근"
        ),
        "onboarding_desc_4" to mapOf(
            "en" to "Storage and audio permissions are requested on first launch to scan your library and power the visualizer.",
            "tr" to "Müzik dosyalarınızı taramak ve ses görselleştiricisini çalıştırmak için gerekli depolama ve ses izinleri ilk açılışta istenir.",
            "es" to "Se solicitan permisos de almacenamiento y audio en el primer inicio para escanear tu biblioteca y usar el visualizador.",
            "fr" to "Les autorisations de stockage et audio sont demandées au premier lancement pour analyser votre bibliothèque.",
            "de" to "Speicher- und Audioberechtigungen werden beim ersten Start angefordert, um deine Bibliothek zu scannen.",
            "it" to "I permessi di archiviazione e audio vengono richiesti al primo avvio per scansionare la libreria e attivare il visualizzatore.",
            "pt" to "Permissões de armazenamento e áudio são solicitadas na primeira inicialização para escanear sua biblioteca.",
            "ru" to "Разрешения на доступ к памяти и звуку запрашиваются при первом запуске для сканирования медиатеки.",
            "ja" to "ライブラリのスキャンとビジュアライザー実行のため、初回起動時にストレージと音声の権限が必要です。",
            "zh" to "首次启动时需要存储和音频权限，用于扫描音乐库并启用音频可视化器。",
            "ar" to "يتم طلب أذونات التخزين والصوت عند التشغيل الأول لمسح مكتبتك وتشغيل المؤثرات البصرية.",
            "ko" to "음악 라이브러리를 검색하고 비주얼라이저를 작동하기 위해 첫 실행 시 저장소 및 오디오 권한이 요청됩니다."
        ),
        "onboarding_title_5" to mapOf(
            "en" to "Let the Journey Begin!", "tr" to "Müzik Yolculuğu Başlasın!", "es" to "¡Que empiece el viaje musical!",
            "fr" to "Que le voyage commence !", "de" to "Lass die Reise beginnen!", "it" to "Che il viaggio abbia inizio!",
            "pt" to "Que a jornada comece!", "ru" to "Пусть путешествие начнется!", "ja" to "音楽の旅を始めましょう！",
            "zh" to "开启音乐之旅！", "ar" to "فلتبدأ رحلة الموسيقى!", "ko" to "음악 여정을 시작하세요!"
        ),
        "onboarding_desc_5" to mapOf(
            "en" to "All your tracks and playlists are ready. Are you set for an unparalleled music experience?",
            "tr" to "Tüm şarkılarınız ve çalma listeleriniz hazır. Eşsiz bir müzik deneyimi için hazır mısınız?",
            "es" to "Todas tus canciones y listas están listas. ¿Preparado para una experiencia musical incomparable?",
            "fr" to "Toutes vos chansons et playlists sont prêtes. Prêt pour une expérience musicale unique ?",
            "de" to "Alle deine Titel und Wiedergabelisten sind bereit. Bereit für ein unvergleichliches Musikerlebnis?",
            "it" to "Tutti i tuoi brani e playlist sono pronti. Pronto per un'esperienza musicale senza pari?",
            "pt" to "Todas as suas faixas e playlists estão prontas. Pronto para uma experiência musical incrível?",
            "ru" to "Все ваши треки и плейлисты готовы. Готовы к непревзойденному музыкальному опыту?",
            "ja" to "すべてのトラックとプレイリストの準備が整いました。比類なき音楽体験の準備はできましたか？",
            "zh" to "所有曲目和播放列表均已就绪。准备好享受前所未有的音乐体验了吗？",
            "ar" to "جميع مساراتك وقوائم التشغيل جاهزة. هل أنت مستعد لتجربة موسيقية لا مثيل لها؟",
            "ko" to "모든 음악과 재생목록이 준비되었습니다. 최고의 음악 경험을 즐길 준비가 되셨나요?"
        ),
        "songs" to mapOf(
            "en" to "Songs", "tr" to "Şarkı", "es" to "Canciones", "fr" to "Morceaux",
            "de" to "Titel", "it" to "Brani", "pt" to "Músicas", "ru" to "Песни",
            "ja" to "曲", "zh" to "首歌曲", "ar" to "أغاني", "ko" to "곡"
        ),
        "now_playing_badge" to mapOf(
            "en" to "NOW PLAYING", "tr" to "ŞİMDİ OYNATILIYOR", "es" to "REPRODUCIENDO",
            "fr" to "EN LECTURE", "de" to "WIRD GESPIELT", "it" to "IN RIPRODUZIONE",
            "pt" to "REPRODUZINDO", "ru" to "СЕЙЧАС ИГРАЕТ", "ja" to "再生中",
            "zh" to "正在播放", "ar" to "قيد التشغيل الآن", "ko" to "현재 재생 중"
        ),
        "share_song" to mapOf(
            "en" to "Share Song", "tr" to "Şarkıyı Paylaş", "es" to "Compartir canción",
            "fr" to "Partager le morceau", "de" to "Titel teilen", "it" to "Condividi brano",
            "pt" to "Compartilhar música", "ru" to "Поделиться песней", "ja" to "曲を共有",
            "zh" to "分享歌曲", "ar" to "مشاركة الأغنية", "ko" to "노래 공유"
        ),
        "now_playing_queue" to mapOf(
            "en" to "Playing Queue", "tr" to "Oynatılan Liste", "es" to "Cola de reproducción",
            "fr" to "File de lecture", "de" to "Wiedergabeschlange", "it" to "Coda di riproduzione",
            "pt" to "Fila de reprodução", "ru" to "Очередь воспроизведения", "ja" to "再生キュー",
            "zh" to "播放队列", "ar" to "قائمة التشغيل الحالية", "ko" to "재생 대기열"
        ),
        "song_info" to mapOf(
            "en" to "Song Info", "tr" to "Şarkı Bilgileri", "es" to "Información de la canción",
            "fr" to "Infos sur le morceau", "de" to "Titel-Info", "it" to "Info brano",
            "pt" to "Informações da música", "ru" to "Информация о песне", "ja" to "曲の情報",
            "zh" to "歌曲信息", "ar" to "معلومات الأغنية", "ko" to "노래 정보"
        ),
        "unknown_artist" to mapOf(
            "en" to "Unknown Artist", "tr" to "Bilinmeyen Sanatçı", "es" to "Artista desconocido",
            "fr" to "Artiste inconnu", "de" to "Unbekannter Künstler", "it" to "Artista sconosciuto",
            "pt" to "Artista desconhecido", "ru" to "Неизвестный исполнитель", "ja" to "不明なアーティスト",
            "zh" to "未知艺术家", "ar" to "فنان غير معروف", "ko" to "알 수 없는 아티스트"
        ),
        "eq_subtitle" to mapOf(
            "en" to "Audio profile & effects", "tr" to "Ses profili ve efektler", "es" to "Perfil de audio y efectos",
            "fr" to "Profil audio et effets", "de" to "Audioprofil & Effekte", "it" to "Profilo audio ed effetti",
            "pt" to "Perfil de áudio e efeitos", "ru" to "Аудиопрофиль и эффекты", "ja" to "オーディオプロファイルとエフェクト",
            "zh" to "音频配置文件与音效", "ar" to "الملف الصوتي والمؤثرات", "ko" to "오디오 프로필 및 효과"
        ),
        "artist_playlist_tag" to mapOf(
            "en" to "Artist Playlist", "tr" to "Sanatçı Çalma Listesi", "es" to "Lista del artista",
            "fr" to "Playlist d'artiste", "de" to "Künstler-Playlist", "it" to "Playlist dell'artista",
            "pt" to "Playlist do artista", "ru" to "Плейлист исполнителя", "ja" to "アーティストプレイリスト",
            "zh" to "艺术家播放列表", "ar" to "قائمة تشغيل الفنان", "ko" to "아티스트 재생목록"
        ),
        "new_playlist_subtitle" to mapOf(
            "en" to "Create a new playlist", "tr" to "Yeni bir çalma listesi oluştur", "es" to "Crear una nueva lista",
            "fr" to "Créer une nouvelle playlist", "de" to "Neue Playlist erstellen", "it" to "Crea una nuova playlist",
            "pt" to "Criar uma nova playlist", "ru" to "Создать новый плейлист", "ja" to "新しいプレイリストを作成",
            "zh" to "创建新播放列表", "ar" to "إنشاء قائمة تشغيل جديدة", "ko" to "새 재생목록 만들기"
        ),
        "all_artist_playlists_exist" to mapOf(
            "en" to "All artist playlists already exist", "tr" to "Tüm sanatçı listeleri zaten mevcut",
            "es" to "Todas las listas de artistas ya existen", "fr" to "Toutes les playlists d'artistes existent déjà",
            "de" to "Alle Künstler-Playlists existieren bereits", "it" to "Tutte le playlist dell'artista esistono già",
            "pt" to "Todas as playlists de artistas já existem", "ru" to "Все плейлисты исполнителей уже существуют",
            "ja" to "すべてのアーティストプレイリストが既に存在します", "zh" to "所有艺术家播放列表已存在",
            "ar" to "جميع قوائم تشغيل الفنانين موجودة بالفعل", "ko" to "모든 아티스트 재생목록이 이미 존재합니다"
        ),
        "artists_found" to mapOf(
            "en" to "artists found", "tr" to "sanatçı bulundu", "es" to "artistas encontrados",
            "fr" to "artistes trouvés", "de" to "Künstler gefunden", "it" to "artisti trovati",
            "pt" to "artistas encontrados", "ru" to "исполнителей найдено", "ja" to "人のアーティストが見つかりました",
            "zh" to "位艺术家已找到", "ar" to "فنان تم العثور عليهم", "ko" to "명의 아티스트를 찾았습니다"
        ),
        "shuffle_on" to mapOf(
            "en" to "Shuffle: On 🔀", "tr" to "Rastgele Çalma: Açık 🔀", "es" to "Aleatorio: Activado 🔀",
            "fr" to "Aléatoire : Activé 🔀", "de" to "Zufall: An 🔀", "it" to "Casuale: Attivo 🔀",
            "pt" to "Aleatório: Ativado 🔀", "ru" to "Случайно: Вкл 🔀", "ja" to "シャッフル: オン 🔀",
            "zh" to "随机播放: 开启 🔀", "ar" to "خلط: تشغيل 🔀", "ko" to "셔플: 켜짐 🔀"
        ),
        "shuffle_off" to mapOf(
            "en" to "Shuffle: Off ➡️", "tr" to "Rastgele Çalma: Kapalı ➡️", "es" to "Aleatorio: Desactivado ➡️",
            "fr" to "Aléatoire : Désactivé ➡️", "de" to "Zufall: Aus ➡️", "it" to "Casuale: Disattivato ➡️",
            "pt" to "Aleatório: Desativado ➡️", "ru" to "Случайно: Выкл ➡️", "ja" to "シャッフル: オフ ➡️",
            "zh" to "随机播放: 关闭 ➡️", "ar" to "خلط: إيقاف ➡️", "ko" to "셔플: 꺼짐 ➡️"
        ),
        "repeat_one" to mapOf(
            "en" to "Repeat One 🔂", "tr" to "Tek Şarkıyı Tekrarla 🔂", "es" to "Repetir una 🔂",
            "fr" to "Répéter un titre 🔂", "de" to "Titel wiederholen 🔂", "it" to "Ripeti brano 🔂",
            "pt" to "Repetir uma 🔂", "ru" to "Повтор одного трека 🔂", "ja" to "1曲リピート 🔂",
            "zh" to "单曲循环 🔂", "ar" to "تكرار أغنية واحدة 🔂", "ko" to "한 곡 반복 🔂"
        ),
        "repeat_all" to mapOf(
            "en" to "Repeat All 🔁", "tr" to "Tümünü Tekrarla 🔁", "es" to "Repetir todo 🔁",
            "fr" to "Tout répéter 🔁", "de" to "Alles wiederholen 🔁", "it" to "Ripeti tutto 🔁",
            "pt" to "Repetir tudo 🔁", "ru" to "Повторять все 🔁", "ja" to "すべてリピート 🔁",
            "zh" to "全部循环 🔁", "ar" to "تكرار الكل 🔁", "ko" to "전체 반복 🔁"
        ),
        "repeat_off" to mapOf(
            "en" to "Repeat Off ➡️", "tr" to "Tekrar Kapalı ➡️", "es" to "Repetición desactivada ➡️",
            "fr" to "Répétition désactivée ➡️", "de" to "Wiederholung aus ➡️", "it" to "Ripetizione disattivata ➡️",
            "pt" to "Repetição desativada ➡️", "ru" to "Повтор выкл ➡️", "ja" to "リピートオフ ➡️",
            "zh" to "关闭循环 ➡️", "ar" to "إيقاف التكرار ➡️", "ko" to "반복 꺼짐 ➡️"
        ),
        "most_played_song" to mapOf(
            "en" to "Most Played Song", "tr" to "En Çok Dinlenen Şarkı", "es" to "Canción más escuchada",
            "fr" to "Morceau le plus écouté", "de" to "Meistgespielter Titel", "it" to "Brano più ascoltato",
            "pt" to "Música mais tocada", "ru" to "Самый популярный трек", "ja" to "最も再生された曲",
            "zh" to "播放最多的歌曲", "ar" to "الأغنية الأكثر تشغيلاً", "ko" to "가장 많이 재생된 곡"
        ),
        "top_5_songs" to mapOf(
            "en" to "Your Top 5 Songs", "tr" to "Top 5 Şarkınız", "es" to "Tus 5 mejores canciones",
            "fr" to "Votre Top 5 de morceaux", "de" to "Deine Top 5 Titel", "it" to "I tuoi migliori 5 brani",
            "pt" to "Suas 5 melhores músicas", "ru" to "Топ-5 ваших песен", "ja" to "トップ5の曲",
            "zh" to "您的前5首歌曲", "ar" to "أفضل 5 أغاني لديك", "ko" to "내 Top 5 노래"
        ),
        "theme_dynamic" to mapOf(
            "en" to "Dynamic Art", "tr" to "Dinamik Albüm", "es" to "Arte dinámico",
            "fr" to "Pochette dynamique", "de" to "Dynamisches Cover", "it" to "Copertina dinamica",
            "pt" to "Capa dinâmica", "ru" to "Динамическая обложка", "ja" to "ダイナミックアート",
            "zh" to "动态封面", "ar" to "غلاف ديناميكي", "ko" to "다이내믹 아트"
        ),
        "theme_amoled" to mapOf(
            "en" to "OLED Black", "tr" to "Zifiri Siyah", "es" to "Negro OLED",
            "fr" to "Noir OLED", "de" to "OLED-Schwarz", "it" to "Nero OLED",
            "pt" to "Preto OLED", "ru" to "Черный OLED", "ja" to "OLEDブラック",
            "zh" to "纯黑 OLED", "ar" to "أسود داكن", "ko" to "OLED 블랙"
        ),
        "theme_dark" to mapOf(
            "en" to "Midnight", "tr" to "Gece Yarısı", "es" to "Medianoche",
            "fr" to "Minuit", "de" to "Mitternacht", "it" to "Mezzanotte",
            "pt" to "Meia-noite", "ru" to "Полночь", "ja" to "ミッドナイト",
            "zh" to "午夜", "ar" to "منتصف الليل", "ko" to "미드나잇"
        ),
        "theme_cyberpunk" to mapOf(
            "en" to "Cyberpunk", "tr" to "Siberpunk", "es" to "Cyberpunk",
            "fr" to "Cyberpunk", "de" to "Cyberpunk", "it" to "Cyberpunk",
            "pt" to "Cyberpunk", "ru" to "Киберпанк", "ja" to "サイバーパンク",
            "zh" to "赛博朋克", "ar" to "سايبربانك", "ko" to "사이버펑크"
        ),
        "theme_rose_gold" to mapOf(
            "en" to "Rose Gold", "tr" to "Roz Altın", "es" to "Oro Rosa",
            "fr" to "Or Rose", "de" to "Roségold", "it" to "Oro Rosa",
            "pt" to "Ouro Rosa", "ru" to "Розовое золото", "ja" to "ローズゴールド",
            "zh" to "玫瑰金", "ar" to "ذهب وردي", "ko" to "로즈 골드"
        ),
        "eq_normal" to mapOf(
            "en" to "Normal", "tr" to "Normal", "es" to "Normal", "fr" to "Normal",
            "de" to "Normal", "it" to "Normale", "pt" to "Normal", "ru" to "Обычный",
            "ja" to "ノーマル", "zh" to "标准", "ar" to "عادي", "ko" to "일반"
        ),
        "eq_bass_boost" to mapOf(
            "en" to "Bass Boost", "tr" to "Bas Güçlendirme", "es" to "Refuerzo de graves",
            "fr" to "Amplification des basses", "de" to "Bassverstärkung", "it" to "Potenziamento bassi",
            "pt" to "Reforço de graves", "ru" to "Усиление басов", "ja" to "低音増幅",
            "zh" to "低音增强", "ar" to "تعزيز الباس", "ko" to "베이스 부스트"
        ),
        "eq_vocal" to mapOf(
            "en" to "Vocal Boost", "tr" to "Vokal", "es" to "Voz", "fr" to "Voix",
            "de" to "Gesang", "it" to "Voce", "pt" to "Voz", "ru" to "Вокал",
            "ja" to "ボーカル", "zh" to "人声增强", "ar" to "تعزيز الصوت", "ko" to "보컬 부스트"
        ),
        "eq_treble" to mapOf(
            "en" to "Treble Boost", "tr" to "Tiz", "es" to "Agudos", "fr" to "Aigus",
            "de" to "Höhen", "it" to "Acuti", "pt" to "Agudos", "ru" to "Высокие частоты",
            "ja" to "高音増幅", "zh" to "高音增强", "ar" to "تعزيز الطبقات العالية", "ko" to "고음 부스트"
        ),
        "eq_loudness" to mapOf(
            "en" to "Loudness", "tr" to "Yüksek Ses", "es" to "Volumen alto", "fr" to "Volume fort",
            "de" to "Lautstärke", "it" to "Volume elevato", "pt" to "Volume alto", "ru" to "Громкость",
            "ja" to "ラウドネス", "zh" to "响度", "ar" to "ارتفاع الصوت", "ko" to "음량 증가"
        ),
        "eq_desc_normal" to mapOf(
            "en" to "Original balanced audio profile", "tr" to "Orijinal dengeli ses profili",
            "es" to "Perfil de audio original equilibrado", "fr" to "Profil audio équilibré d'origine",
            "de" to "Originales ausgewogenes Audioprofil", "it" to "Profilo audio originale bilanciato",
            "pt" to "Perfil de áudio balanceado original", "ru" to "Оригинальный сбалансированный профиль",
            "ja" to "バランスのとれたオリジナル音響", "zh" to "原始均衡音频模式",
            "ar" to "الملف الصوتي الأصلي المتوازن", "ko" to "원음 균형 오디오 프로필"
        ),
        "eq_desc_bass_boost" to mapOf(
            "en" to "Deep low-end punch & enhanced rhythm", "tr" to "Derin bas vuruşları ve güçlü ritim",
            "es" to "Graves profundos y ritmo mejorado", "fr" to "Basses profondes et rythme amplifié",
            "de" to "Tiefer Bass und verstärkter Rhythmus", "it" to "Bassi profondi e ritmo amplificato",
            "pt" to "Batidas graves profundas e ritmo aprimorado", "ru" to "Глубокий плотный бас и ритм",
            "ja" to "重厚な重低音と力強いリズム", "zh" to "浑厚低音冲击与增强节拍",
            "ar" to "نبضات باس عميقة وإيقاع قوي", "ko" to "깊은 저음 타격감과 강화된 리듬"
        ),
        "eq_desc_vocal" to mapOf(
            "en" to "Crystal clear vocals & speech clarity", "tr" to "Net ve ön planda vokaller",
            "es" to "Voces nítidas y claridad de voz", "fr" to "Voix cristallines et clarté des paroles",
            "de" to "Kristallklarer Gesang und Sprachverständlichkeit", "it" to "Voci cristalline e massima chiarezza",
            "pt" to "Vocais nítidos e clareza de fala", "ru" to "Кристально чистый вокал и разборчивость",
            "ja" to "透き通るようなボーカルとクリアな声", "zh" to "晶莹剔透的人声与清晰对白",
            "ar" to "أصوات واضحة ونقاء صوتي فائق", "ko" to "선명하고 또렷한 보컬 사운드"
        ),
        "eq_desc_treble" to mapOf(
            "en" to "Bright highs & crisp instrument details", "tr" to "Parlak tizler ve enstrüman detayları",
            "es" to "Agudos brillantes y detalles nítidos de instrumentos", "fr" to "Aigus éclatants et détails instrumentaux nets",
            "de" to "Brillante Höhen und klare Instrumentendetails", "it" to "Alti brillanti e dettagli nitidi degli strumenti",
            "pt" to "Agudos brilhantes e detalhes instrumentais nítidos", "ru" to "Яркие высокие частоты и детали инструментов",
            "ja" to "煌びやかな高音と繊細な楽器ディテール", "zh" to "明亮高音与清脆乐器细节",
            "ar" to "طبقات صوت عالية ومشرقة وتفاصيل آلات نقية", "ko" to "밝은 고음과 선명한 악기 디테일"
        ),
        "eq_desc_loudness" to mapOf(
            "en" to "Maximized volume & rich fullness", "tr" to "Maksimum ses seviyesi ve dolgunluk",
            "es" to "Volumen maximizado y gran plenitud", "fr" to "Volume maximal et richesse sonore",
            "de" to "Maximale Lautstärke und volle Klangfülle", "it" to "Volume massimizzato e ricchezza del suono",
            "pt" to "Volume maximizado e riqueza sonora", "ru" to "Максимальная громкость и насыщенность",
            "ja" to "最大化された音量と豊かな響き", "zh" to "最大音量与饱满丰沛音质",
            "ar" to "أقصى مستوى صوت وامتلاء غني", "ko" to "최대화된 음량과 풍부한 공간감"
        )
    )

    fun get(key: String): String {
        val keyMap = translations[key] ?: return key
        return keyMap[lang.code] ?: keyMap["en"] ?: keyMap["tr"] ?: key
    }

    fun getThemeLabel(theme: AppTheme): String = when (theme) {
        AppTheme.DYNAMIC -> get("theme_dynamic")
        AppTheme.AMOLED -> get("theme_amoled")
        AppTheme.DARK -> get("theme_dark")
        AppTheme.NEON_CYBER -> get("theme_cyberpunk")
        AppTheme.ROSE_GOLD -> get("theme_rose_gold")
    }

    fun getEQPresetLabel(preset: EQPreset): String = when (preset) {
        EQPreset.NONE -> get("eq_normal")
        EQPreset.BASS_BOOST -> get("eq_bass_boost")
        EQPreset.VOCAL -> get("eq_vocal")
        EQPreset.TREBLE -> get("eq_treble")
        EQPreset.LOUDNESS -> get("eq_loudness")
    }

    fun getEQPresetDesc(preset: EQPreset): String = when (preset) {
        EQPreset.NONE -> get("eq_desc_normal")
        EQPreset.BASS_BOOST -> get("eq_desc_bass_boost")
        EQPreset.VOCAL -> get("eq_desc_vocal")
        EQPreset.TREBLE -> get("eq_desc_treble")
        EQPreset.LOUDNESS -> get("eq_desc_loudness")
    }
}

object MusicQuotes {
    val quotes = listOf(
        "Müzik ruhun gıdasıdır. ✨",
        "Sözlerin bittiği yerde müzik başlar. 🎶",
        "Hayat müziksiz bir hata olurdu. 🎧",
        "Müzik, evrenin ortak dilidir. 🌍",
        "Ritim, kalbin en sadık dostudur. 💓",
        "Her şarkının anlatacak bir hikayesi vardır. 📖",
        "Müzik, görünmeyen ruhun sesidir. 🌌",
        "Kulaklıkla dünyayı sessize al, müziğin sesini aç. 🚀",
        "Melodiler geçmişe atılan en hızlı köprüdür. 🌉",
        "Müzik, kalbe dokunmanın en güzel yoludur. 🎹",
        "Şarkılar, kelimelerin söyleyemediklerini fısıldar. 🌙",
        "Müziksiz bir gün, kayıp bir gündür. ⏳",
        "Notalar, duyguların en saf halidir. 🎼",
        "Bir şarkı çalınır ve tüm anılar canlanır. 🌟",
        "Müzik, ruhun özgürlük ilan ettiği andır. 🦅",
        "Kulaklığı tak ve hayatın ritmine ayak uydur. 🎸",
        "Sessizliği en güzel süsleyen şey melodidir. 🎻",
        "Müzik, en karanlık gecelerde yanan ışıktır. 🔦",
        "Şarkı söylemek, ruhun nefes almasıdır. 🌬️",
        "Her ritim, yaşamın gizli bir nabzıdır. ⚡",
        "Müzik, yaraları saran görünmez bir eldir. 🤲",
        "Hayatın en güzel anlarının arka planında hep bir şarkı vardır. 🌅",
        "Notaların büyücülüğüne teslim ol. ✨",
        "Müzik, kalbin attığı en doğru tempodur. 🥁",
        "Zamanı durdurmanın tek yolu iyi bir şarkıdır. ⏳",
        "Müzik, ruhun derinliklerine yapılan bir yolculuktur. 🚀",
        "Şarkılar insanın içindeki aynadır. 🪞",
        "Müzik varsa, asla yalnız değilsin. 🤝",
        "Ruhunu besleyen melodiye kulak ver. 🌿",
        "Müzik, kelimelerin yetersiz kaldığı yerde konuşur. 💫",
        "Her akor, yeni bir duygunun başlangıcıdır. 🎸",
        "Hayatın ritmini kaçırma, müziği aç. 📻",
        "Müzik, zihni dinlendiren en güzel sığınaktır. 🏰",
        "Şarkılar kalbin en gizli odalarını havalandırır. 🪟",
        "Müzik, evrenin ritmiyle kalbin ritminin buluştuğu yerdir. 💞",
        "En iyi terapist, kulaklığındaki en sevdiğin şarkıdır. 🛋️",
        "Müzik, ölümsüzlüğü hissetmenin en kolay yoludur. 🕊️",
        "Şarkılar, hayatın soundtrack'idir. 🎬",
        "Müzik, ruhun dans ettiği dildir. 💃",
        "Kulaklığı tak, dış dünyayı kapat. 🔕",
        "Melodiler içimizdeki çocukla konuşur. 🧸",
        "Müzik, nehrin akışına kapılmak gibidir. 🌊",
        "Her şarkı yeni bir evrene açılan kapıdır. 🚪",
        "Müzik, kalbin en saf yankısıdır. 🔔",
        "Hayat bir şarkıyla başlar, kalple devam eder. 💖",
        "Müzik, hüzne neşe katan sihirli bir iksirdir. 🧪",
        "Kulaklığındaki ses, gününün rengini belirler. 🎨",
        "Müzik, en sadık yol arkadaşıdır. 🛤️",
        "Notalar konuşmaya başladığında susup dinlemelisin. 🤫",
        "Müzik, ruhun evrenle kurduğu gizli bağdır. 🌌",
        "Şarkılar kalbin en derin yaralarını sarar. 🩹",
        "Müzik, zihnin en güzel kaçış noktasıdır. 🏝️",
        "Ritim varsa, umut hep vardır. 🌱",
        "Müzik, kelimelere ihtiyaç duymayan şiirdir. 📜",
        "Her melodi, ruhun çizdiği bir tablodur. 🖼️",
        "Müzik, hayatın en tatlı melodramıdır. 🎭",
        "Kulaklığını tak ve kendi konserini ver. 🎤",
        "Müzik, zamanın ve mekanın ötesine geçmektir. ⏳",
        "Şarkılar, ruhun parmak izleridir. 🔍",
        "Müzik, kalbin en dürüst itirafıdır. 💌",
        "Hayat bir şarkıdır, sen de en güzel notası ol. 🌟",
        "Müzik, içimizdeki fırtınayı dindiren rüzgardır. 🍃",
        "Notaların rehberliğinde kaybolmaya hazır mısın? 🧭",
        "Müzik, evrenin kalbinin attığı yerdir. 💓",
        "Şarkılar, en iyi anı saklama kutusudur. 📦",
        "Müzik, ruhun uçsuz bucaksız denizidir. ⛵",
        "Kulaklığını tak ve içindeki müziği serbest bırak. 🦋",
        "Müzik, gözler kapalıyken dünyayı görmektir. 👁️‍🗨️",
        "Her şarkı yeni bir macera, yeni bir duygu. 🗺️",
        "Müzik, en güçlü anı toplayıcısıdır. 🌺",
        "Şarkılar, kalbin ritmiyle eşzamanlı atan melodi kasetidir. 📼",
        "Müzik, ruhun en özel lisanıdır. 🌐",
        "Hayat kısa, müziği son ses aç. 🔊",
        "Müzik, karanlıkta yol gösteren kuzey yıldızıdır. ⭐",
        "Şarkılar insanın en yakın sırdaşıdır. 🤫",
        "Müzik, evrenin en güzel uyumudur. 🎶",
        "Kulaklığını tak ve sadece müziği hisset. 🧘",
        "Müzik, ruhun yaşlanmasını engelleyen iksirdir. 💧",
        "Her melodi, geleceğe bırakılan en güzel mirastır. 🎁",
        "Müzik, kalbin en güzel şarkısını söylemesidir. 🎤"
    )

    fun getRandomQuote(): String = quotes.random()
}
