package com.blackstack.beatbox

import android.net.Uri
import androidx.compose.ui.graphics.Color

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
    val isGradient: Boolean = false,
    val gradientColors: List<Color> = emptyList()
) {
    // Simple, Sweet Solid Color Themes
    DARK("Dark Mode", "Koyu Mod", true, Color(0xFF121212)),
    LIGHT("Light Mode", "Açık Mod", false, Color(0xFFF5F5F5)),
    PURPLE("Purple Dream", "Mor Rüya", true, Color(0xFF6200EA)),
    PINK("Sweet Pink", "Tatlı Pembe", true, Color(0xFFE91E63)),
    BLUE("Ocean Blue", "Okyanus Mavisi", true, Color(0xFF2196F3)),
    TEAL("Fresh Teal", "Ferah Turkuaz", true, Color(0xFF009688)),
    GREEN("Nature Green", "Doğa Yeşili", true, Color(0xFF4CAF50)),
    ORANGE("Warm Orange", "Sıcak Turuncu", true, Color(0xFFFF9800)),
    RED("Passion Red", "Tutku Kırmızısı", true, Color(0xFFF44336)),
    INDIGO("Deep Indigo", "Derin İndigo", true, Color(0xFF3F51B5))
}

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumId: Long,
    val folderName: String,
    val dateAdded: Long
)

data class Playlist(
    var name: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val songIds: MutableList<Long> = mutableListOf(),
    val isSmart: Boolean = false
)

enum class EQPreset(val label: String, val labelTR: String) {
    NONE("Normal", "Normal"),
    BASS_BOOST("Bass Boost", "Bas Güçlendirme"),
    VOCAL("Vocal Boost", "Vokal"),
    TREBLE("Treble Boost", "Tiz"),
    LOUDNESS("Loudness", "Yüksek Ses")
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
            "en" to "BeatBox User Agreement\n\nLast Updated: 2025\n\n1. General Terms\nBy using the BeatBox application, you agree to this agreement. The app is designed to play music files on your device.\n\n2. Privacy and Data Usage\n• BeatBox requires permission to access music files on your device.\n• No personal data or music files are sent to our servers.\n• All data is stored locally on your device.\n• No internet connection required.\n\n3. Permissions\n• Music and Audio Files: Required to access your music library.\n• Microphone (Visualizer): Used only for audio visualization, no recording.\n• Notifications: Required for music control.\n\n4. Copyright\n• Users must be the legal owner of music files on their devices.\n• BeatBox is not responsible for copyright violations.\n• Only listen to music you legally own.\n\n5. Disclaimer\n• The app is provided \"as is\".\n• We are not responsible for data loss or device damage.\n• You use the app at your own risk.\n\n6. Changes\nThis agreement may be updated at any time. Updates will be announced within the app.\n\n7. Contact\nFor questions: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Developed by BlackStack Labs.",
            "tr" to "BeatBox Kullanıcı Sözleşmesi\n\nSon Güncelleme: 2025\n\n1. Genel Koşullar\nBeatBox uygulamasını kullanarak bu sözleşmeyi kabul etmiş sayılırsınız. Uygulama, cihazınızdaki müzik dosyalarını oynatmak için tasarlanmıştır.\n\n2. Gizlilik ve Veri Kullanımı\n• BeatBox, cihazınızdaki müzik dosyalarına erişim izni gerektirir.\n• Hiçbir kişisel veri veya müzik dosyası sunucularımıza gönderilmez.\n• Tüm veriler cihazınızda yerel olarak saklanır.\n• İnternet bağlantısı gerektirmez.\n\n3. İzinler\n• Müzik ve Ses Dosyaları: Müzik kütüphanenize erişim için gereklidir.\n• Mikrofon (Visualizer): Sadece ses görselleştirme için kullanılır, kayıt yapılmaz.\n• Bildirimler: Müzik kontrolü için gereklidir.\n\n4. Telif Hakları\n• Kullanıcılar, cihazlarındaki müzik dosyalarının yasal sahibi olmalıdır.\n• BeatBox, telif hakkı ihlallerinden sorumlu değildir.\n• Sadece yasal olarak sahip olduğunuz müzikleri dinleyin.\n\n5. Sorumluluk Reddi\n• Uygulama \"olduğu gibi\" sunulmaktadır.\n• Veri kaybı veya cihaz hasarından sorumlu değiliz.\n• Uygulamayı kendi sorumluluğunuzda kullanırsınız.\n\n6. Değişiklikler\nBu sözleşme herhangi bir zamanda güncellenebilir. Güncellemeler uygulama içinde duyurulacaktır.\n\n7. İletişim\nSorularınız için: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - BlackStack Labs tarafından geliştirilmiştir.",
            "es" to "Acuerdo de Usuario de BeatBox\n\nÚltima actualización: 2025\n\n1. Términos Generales\nAl usar la aplicación BeatBox, aceptas este acuerdo. La aplicación está diseñada para reproducir archivos de música en tu dispositivo.\n\n2. Privacidad y Uso de Datos\n• BeatBox requiere permiso para acceder a archivos de música en tu dispositivo.\n• No se envían datos personales ni archivos de música a nuestros servidores.\n• Todos los datos se almacenan localmente en tu dispositivo.\n• No se requiere conexión a Internet.\n\n3. Permisos\n• Archivos de Música y Audio: Requerido para acceder a tu biblioteca de música.\n• Micrófono (Visualizador): Usado solo para visualización de audio, sin grabación.\n• Notificaciones: Requerido para control de música.\n\n4. Derechos de Autor\n• Los usuarios deben ser propietarios legales de los archivos de música en sus dispositivos.\n• BeatBox no es responsable de violaciones de derechos de autor.\n• Solo escucha música que poseas legalmente.\n\n5. Descargo de Responsabilidad\n• La aplicación se proporciona \"tal cual\".\n• No somos responsables de pérdida de datos o daños al dispositivo.\n• Usas la aplicación bajo tu propio riesgo.\n\n6. Cambios\nEste acuerdo puede actualizarse en cualquier momento. Las actualizaciones se anunciarán dentro de la aplicación.\n\n7. Contacto\nPara preguntas: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Desarrollado por BlackStack Labs.",
            "fr" to "Accord d'Utilisateur BeatBox\n\nDernière mise à jour: 2025\n\n1. Conditions Générales\nEn utilisant l'application BeatBox, vous acceptez cet accord. L'application est conçue pour lire des fichiers musicaux sur votre appareil.\n\n2. Confidentialité et Utilisation des Données\n• BeatBox nécessite l'autorisation d'accéder aux fichiers musicaux sur votre appareil.\n• Aucune donnée personnelle ou fichier musical n'est envoyé à nos serveurs.\n• Toutes les données sont stockées localement sur votre appareil.\n• Aucune connexion Internet requise.\n\n3. Autorisations\n• Fichiers Musicaux et Audio: Requis pour accéder à votre bibliothèque musicale.\n• Microphone (Visualiseur): Utilisé uniquement pour la visualisation audio, pas d'enregistrement.\n• Notifications: Requis pour le contrôle de la musique.\n\n4. Droits d'Auteur\n• Les utilisateurs doivent être propriétaires légaux des fichiers musicaux sur leurs appareils.\n• BeatBox n'est pas responsable des violations de droits d'auteur.\n• N'écoutez que la musique que vous possédez légalement.\n\n5. Clause de Non-Responsabilité\n• L'application est fournie \"telle quelle\".\n• Nous ne sommes pas responsables de la perte de données ou des dommages à l'appareil.\n• Vous utilisez l'application à vos propres risques.\n\n6. Modifications\nCet accord peut être mis à jour à tout moment. Les mises à jour seront annoncées dans l'application.\n\n7. Contact\nPour les questions: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Développé par BlackStack Labs.",
            "de" to "BeatBox Benutzervereinbarung\n\nLetzte Aktualisierung: 2025\n\n1. Allgemeine Bedingungen\nDurch die Nutzung der BeatBox-Anwendung stimmen Sie dieser Vereinbarung zu. Die App ist zum Abspielen von Musikdateien auf Ihrem Gerät konzipiert.\n\n2. Datenschutz und Datennutzung\n• BeatBox benötigt die Berechtigung zum Zugriff auf Musikdateien auf Ihrem Gerät.\n• Keine persönlichen Daten oder Musikdateien werden an unsere Server gesendet.\n• Alle Daten werden lokal auf Ihrem Gerät gespeichert.\n• Keine Internetverbindung erforderlich.\n\n3. Berechtigungen\n• Musik- und Audiodateien: Erforderlich für den Zugriff auf Ihre Musikbibliothek.\n• Mikrofon (Visualizer): Nur für Audio-Visualisierung verwendet, keine Aufnahme.\n• Benachrichtigungen: Erforderlich für Musiksteuerung.\n\n4. Urheberrecht\n• Benutzer müssen rechtmäßige Eigentümer der Musikdateien auf ihren Geräten sein.\n• BeatBox ist nicht verantwortlich für Urheberrechtsverletzungen.\n• Hören Sie nur Musik, die Sie rechtmäßig besitzen.\n\n5. Haftungsausschluss\n• Die App wird \"wie besehen\" bereitgestellt.\n• Wir sind nicht verantwortlich für Datenverlust oder Geräteschäden.\n• Sie verwenden die App auf eigenes Risiko.\n\n6. Änderungen\nDiese Vereinbarung kann jederzeit aktualisiert werden. Aktualisierungen werden in der App angekündigt.\n\n7. Kontakt\nFür Fragen: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Entwickelt von BlackStack Labs.",
            "it" to "Accordo Utente BeatBox\n\nUltimo aggiornamento: 2025\n\n1. Termini Generali\nUtilizzando l'applicazione BeatBox, accetti questo accordo. L'app è progettata per riprodurre file musicali sul tuo dispositivo.\n\n2. Privacy e Utilizzo dei Dati\n• BeatBox richiede l'autorizzazione per accedere ai file musicali sul tuo dispositivo.\n• Nessun dato personale o file musicale viene inviato ai nostri server.\n• Tutti i dati sono archiviati localmente sul tuo dispositivo.\n• Nessuna connessione Internet richiesta.\n\n3. Autorizzazioni\n• File Musicali e Audio: Richiesto per accedere alla tua libreria musicale.\n• Microfono (Visualizzatore): Utilizzato solo per la visualizzazione audio, nessuna registrazione.\n• Notifiche: Richiesto per il controllo della musica.\n\n4. Copyright\n• Gli utenti devono essere proprietari legali dei file musicali sui loro dispositivi.\n• BeatBox non è responsabile per violazioni del copyright.\n• Ascolta solo musica che possiedi legalmente.\n\n5. Esclusione di Responsabilità\n• L'app è fornita \"così com'è\".\n• Non siamo responsabili per perdita di dati o danni al dispositivo.\n• Utilizzi l'app a tuo rischio.\n\n6. Modifiche\nQuesto accordo può essere aggiornato in qualsiasi momento. Gli aggiornamenti saranno annunciati nell'app.\n\n7. Contatto\nPer domande: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Sviluppato da BlackStack Labs.",
            "pt" to "Acordo do Usuário BeatBox\n\nÚltima atualização: 2025\n\n1. Termos Gerais\nAo usar o aplicativo BeatBox, você concorda com este acordo. O aplicativo foi projetado para reproduzir arquivos de música em seu dispositivo.\n\n2. Privacidade e Uso de Dados\n• BeatBox requer permissão para acessar arquivos de música em seu dispositivo.\n• Nenhum dado pessoal ou arquivo de música é enviado para nossos servidores.\n• Todos os dados são armazenados localmente em seu dispositivo.\n• Nenhuma conexão com a Internet necessária.\n\n3. Permissões\n• Arquivos de Música e Áudio: Necessário para acessar sua biblioteca de música.\n• Microfone (Visualizador): Usado apenas para visualização de áudio, sem gravação.\n• Notificações: Necessário para controle de música.\n\n4. Direitos Autorais\n• Os usuários devem ser proprietários legais dos arquivos de música em seus dispositivos.\n• BeatBox não é responsável por violações de direitos autorais.\n• Ouça apenas música que você possui legalmente.\n\n5. Isenção de Responsabilidade\n• O aplicativo é fornecido \"como está\".\n• Não somos responsáveis por perda de dados ou danos ao dispositivo.\n• Você usa o aplicativo por sua própria conta e risco.\n\n6. Alterações\nEste acordo pode ser atualizado a qualquer momento. As atualizações serão anunciadas no aplicativo.\n\n7. Contato\nPara perguntas: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Desenvolvido por BlackStack Labs.",
            "ru" to "Пользовательское соглашение BeatBox\n\nПоследнее обновление: 2025\n\n1. Общие условия\nИспользуя приложение BeatBox, вы соглашаетесь с этим соглашением. Приложение предназначено для воспроизведения музыкальных файлов на вашем устройстве.\n\n2. Конфиденциальность и использование данных\n• BeatBox требует разрешения на доступ к музыкальным файлам на вашем устройстве.\n• Никакие личные данные или музыкальные файлы не отправляются на наши серверы.\n• Все данные хранятся локально на вашем устройстве.\n• Подключение к Интернету не требуется.\n\n3. Разрешения\n• Музыкальные и аудиофайлы: Требуется для доступа к вашей музыкальной библиотеке.\n• Микрофон (Визуализатор): Используется только для визуализации звука, без записи.\n• Уведомления: Требуется для управления музыкой.\n\n4. Авторские права\n• Пользователи должны быть законными владельцами музыкальных файлов на своих устройствах.\n• BeatBox не несет ответственности за нарушения авторских прав.\n• Слушайте только музыку, которой вы законно владеете.\n\n5. Отказ от ответственности\n• Приложение предоставляется \"как есть\".\n• Мы не несем ответственности за потерю данных или повреждение устройства.\n• Вы используете приложение на свой страх и риск.\n\n6. Изменения\nЭто соглашение может быть обновлено в любое время. Обновления будут объявлены в приложении.\n\n7. Контакт\nПо вопросам: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - Разработано BlackStack Labs.",
            "ja" to "BeatBoxユーザー契約\n\n最終更新: 2025\n\n1. 一般条件\nBeatBoxアプリケーションを使用することにより、本契約に同意したことになります。このアプリは、デバイス上の音楽ファイルを再生するために設計されています。\n\n2. プライバシーとデータ使用\n• BeatBoxは、デバイス上の音楽ファイルへのアクセス許可が必要です。\n• 個人データや音楽ファイルは当社のサーバーに送信されません。\n• すべてのデータはデバイスにローカルに保存されます。\n• インターネット接続は不要です。\n\n3. 許可\n• 音楽およびオーディオファイル: 音楽ライブラリへのアクセスに必要です。\n• マイク（ビジュアライザー）: オーディオの視覚化にのみ使用され、録音はされません。\n• 通知: 音楽コントロールに必要です。\n\n4. 著作権\n• ユーザーは、デバイス上の音楽ファイルの合法的な所有者でなければなりません。\n• BeatBoxは著作権侵害について責任を負いません。\n• 合法的に所有している音楽のみを聴いてください。\n\n5. 免責事項\n• アプリは「現状のまま」提供されます。\n• データの損失やデバイスの損傷について責任を負いません。\n• アプリの使用は自己責任で行ってください。\n\n6. 変更\n本契約はいつでも更新される可能性があります。更新はアプリ内で発表されます。\n\n7. お問い合わせ\nご質問は: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - BlackStack Labsによって開発されました。",
            "zh" to "BeatBox用户协议\n\n最后更新: 2025\n\n1. 一般条款\n使用BeatBox应用程序即表示您同意本协议。该应用程序旨在播放您设备上的音乐文件。\n\n2. 隐私和数据使用\n• BeatBox需要访问您设备上的音乐文件的权限。\n• 不会将任何个人数据或音乐文件发送到我们的服务器。\n• 所有数据都本地存储在您的设备上。\n• 不需要互联网连接。\n\n3. 权限\n• 音乐和音频文件: 访问您的音乐库所需。\n• 麦克风（可视化器）: 仅用于音频可视化，不进行录音。\n• 通知: 音乐控制所需。\n\n4. 版权\n• 用户必须是其设备上音乐文件的合法所有者。\n• BeatBox不对版权侵权负责。\n• 只听您合法拥有的音乐。\n\n5. 免责声明\n• 该应用程序按\"原样\"提供。\n• 我们不对数据丢失或设备损坏负责。\n• 您使用该应用程序的风险自负。\n\n6. 更改\n本协议可能随时更新。更新将在应用程序内公布。\n\n7. 联系方式\n如有问题: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - 由BlackStack Labs开发。",
            "ar" to "اتفاقية مستخدم BeatBox\n\nآخر تحديث: 2025\n\n1. الشروط العامة\nباستخدام تطبيق BeatBox، فإنك توافق على هذه الاتفاقية. التطبيق مصمم لتشغيل ملفات الموسيقى على جهازك.\n\n2. الخصوصية واستخدام البيانات\n• يتطلب BeatBox إذنًا للوصول إلى ملفات الموسيقى على جهازك.\n• لا يتم إرسال أي بيانات شخصية أو ملفات موسيقى إلى خوادمنا.\n• يتم تخزين جميع البيانات محليًا على جهازك.\n• لا يلزم الاتصال بالإنترنت.\n\n3. الأذونات\n• ملفات الموسيقى والصوت: مطلوب للوصول إلى مكتبة الموسيقى الخاصة بك.\n• الميكروفون (المصور): يستخدم فقط لتصور الصوت، بدون تسجيل.\n• الإشعارات: مطلوب للتحكم في الموسيقى.\n\n4. حقوق النشر\n• يجب أن يكون المستخدمون المالكين القانونيين لملفات الموسيقى على أجهزتهم.\n• BeatBox غير مسؤول عن انتهاكات حقوق النشر.\n• استمع فقط إلى الموسيقى التي تمتلكها بشكل قانوني.\n\n5. إخلاء المسؤولية\n• يتم توفير التطبيق \"كما هو\".\n• نحن غير مسؤولين عن فقدان البيانات أو تلف الجهاز.\n• أنت تستخدم التطبيق على مسؤوليتك الخاصة.\n\n6. التغييرات\nيمكن تحديث هذه الاتفاقية في أي وقت. سيتم الإعلان عن التحديثات داخل التطبيق.\n\n7. الاتصال\nللأسئلة: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - تم التطوير بواسطة BlackStack Labs.",
            "ko" to "BeatBox 사용자 계약\n\n마지막 업데이트: 2025\n\n1. 일반 조건\nBeatBox 애플리케이션을 사용함으로써 본 계약에 동의하는 것입니다. 이 앱은 기기의 음악 파일을 재생하도록 설계되었습니다.\n\n2. 개인정보 보호 및 데이터 사용\n• BeatBox는 기기의 음악 파일에 액세스할 수 있는 권한이 필요합니다.\n• 개인 데이터나 음악 파일은 당사 서버로 전송되지 않습니다.\n• 모든 데이터는 기기에 로컬로 저장됩니다.\n• 인터넷 연결이 필요하지 않습니다.\n\n3. 권한\n• 음악 및 오디오 파일: 음악 라이브러리에 액세스하는 데 필요합니다.\n• 마이크(시각화 도구): 오디오 시각화에만 사용되며 녹음되지 않습니다.\n• 알림: 음악 제어에 필요합니다.\n\n4. 저작권\n• 사용자는 기기의 음악 파일의 합법적인 소유자여야 합니다.\n• BeatBox는 저작권 침해에 대해 책임지지 않습니다.\n• 합법적으로 소유한 음악만 들으십시오.\n\n5. 면책 조항\n• 앱은 \"있는 그대로\" 제공됩니다.\n• 데이터 손실이나 기기 손상에 대해 책임지지 않습니다.\n• 앱 사용은 본인의 책임입니다.\n\n6. 변경 사항\n본 계약은 언제든지 업데이트될 수 있습니다. 업데이트는 앱 내에서 공지됩니다.\n\n7. 연락처\n문의 사항: dev.blackstackslabs@gmail.com\n\nBeatBox v2.5 - BlackStack Labs에서 개발했습니다."
        ),
        "read_full_agreement" to mapOf(
            "en" to "Read Full Agreement", "tr" to "Tam Sözleşmeyi Oku", "es" to "Leer acuerdo completo",
            "fr" to "Lire l'accord complet", "de" to "Vollständige Vereinbarung lesen", "it" to "Leggi accordo completo",
            "pt" to "Ler acordo completo", "ru" to "Прочитать полное соглашение", "ja" to "完全な契約を読む",
            "zh" to "阅读完整协议", "ar" to "قراءة الاتفاقية الكاملة", "ko" to "전체 계약 읽기"
        ),
        "about_desc" to mapOf(
            "en" to "BeatBox v2.5\nDeveloped by BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "tr" to "BeatBox v2.5\nBlackStack Labs tarafından geliştirilmiştir\n\nGörüşleriniz için: dev.blackstackslabs@gmail.com",
            "es" to "BeatBox v2.5\nDesarrollado por BlackStack Labs\n\nComentarios: dev.blackstackslabs@gmail.com",
            "fr" to "BeatBox v2.5\nDéveloppé par BlackStack Labs\n\nRetour: dev.blackstackslabs@gmail.com",
            "de" to "BeatBox v2.5\nEntwickelt von BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "it" to "BeatBox v2.5\nSviluppato da BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "pt" to "BeatBox v2.5\nDesenvolvido por BlackStack Labs\n\nFeedback: dev.blackstackslabs@gmail.com",
            "ru" to "BeatBox v2.5\nРазработано BlackStack Labs\n\nОбратная связь: dev.blackstackslabs@gmail.com",
            "ja" to "BeatBox v2.5\nBlackStack Labsによって開発\n\nフィードバック: dev.blackstackslabs@gmail.com",
            "zh" to "BeatBox v2.5\n由BlackStack Labs开发\n\n反馈: dev.blackstackslabs@gmail.com",
            "ar" to "BeatBox v2.5\nتم التطوير بواسطة BlackStack Labs\n\nالملاحظات: dev.blackstackslabs@gmail.com",
            "ko" to "BeatBox v2.5\nBlackStack Labs에서 개발\n\n피드백: dev.blackstackslabs@gmail.com"
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
        )
    )

    fun get(key: String): String {
        return translations[key]?.get(lang.code) ?: key
    }
}
