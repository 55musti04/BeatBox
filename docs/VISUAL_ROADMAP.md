# BeatBox — Görsel Dönüşüm Yol Haritası

> **Vizyon:** “Aurora Glass” — albüm kapağından doğan renkler, cam yüzeyler, sinematik geçişler ve akıcı mikro-etkileşimler.
> **Sürüm:** 1.0 · 23 Ekim 2026
> **Kapsam:** Splash, onboarding, ana sayfa, kütüphane, çalma listeleri, mini player, tam ekran player, şarkı sözleri, ayarlar ve tüm diyalog/alt sayfalar.

---

## 0. Yönetici Özeti

BeatBox bugün **çalışan, özellik açısından zengin** bir müzikçalar. Ancak görsel dil parça parça: bazı ekranlar cam efektli ve iddialı, bazıları standart Material kartlardan oluşuyor. Animasyonlar çoğunlukla `tween`; ortak bir hareket dili, tipografi ölçeği ve yüzey hiyerarşisi yok. En büyük fırsat, **mevcut özellikleri yeniden tasarlamak değil, tek bir tasarım sistemi altında birleştirmek**.

Önerilen plan 6 fazdan ve yaklaşık **18–23 iş gününden** oluşuyor:

| Faz | Konu | Süre | Görsel etki |
|---|---|---|---|
| 0 | Temel temizlik + tasarım tokenları + Compose güncellemesi | 2–3 gün | Altyapı |
| 1 | Ambient kabuk: splash, navigasyon, mini player, sayfa geçişleri | 3–4 gün | ★★★★☆ |
| 2 | Ana sayfa + kütüphane + çalma listeleri | 4–5 gün | ★★★★★ |
| 3 | Tam ekran player + şarkı sözleri cilası | 4–5 gün | ★★★★★ |
| 4 | Delight katmanı: mikro-etkileşimler, tema stüdyosu, onboarding | 2–3 gün | ★★★★☆ |
| 5 | Performans, erişilebilirlik, görsel regresyon testleri, release | 2–3 gün | ★★★☆☆ |

**İlk önerilen hamle:** Faz 0 + Faz 1. Bu kombinasyon, uygulamanın “ilk 5 saniyede” verdiği izlenimi en yüksek verimle değiştirir.

---

## 1. Mevcut Durum Denetimi

### 1.1 Güçlü yanlar

- **Albüm kapağından renk çıkarma** (`Palette`) hem ana temada hem player’da var; dinamik tema altyapısı hazır.
- **4 hazır tema** (`DYNAMIC`, `AMOLED`, `NEON_CYBER`, `ROSE_GOLD`) ve 12 dil desteği.
- **Şarkı sözleri ekranı** zaten Apple Music tarzına yakın: blur arka plan, karaoke vurgusu, kelime animasyonları.
- Mini player, tam ekran player ve alt navigasyon **cam yüzey** estetiğine yönelmiş durumda.
- `AlbumArtCache`, `ArtistImageProvider`, `MusicVisualizer` gibi görsel altyapılar mevcut.
- Ses odaklı özellikler (EQ, uyku zamanlayıcı, hız, crossfade, shuffle) görsel olarak da işlenebilecek zenginlikte.

### 1.2 Zayıf noktalar (kod kanıtlarıyla)

| Alan | Bulgu | Etki |
|---|---|---|
| Tema | İki ayrı tema sistemi var: `MainActivity.BeatBoxTheme` + `getThemeColors()` ve kullanılmayan `ui/theme/Theme.kt`. | Renk tutarsızlığı, bakım yükü |
| Dosya yapısı | `Screens.kt` ~3.150 satır; `BeatBoxApp`, yerel `FullScreenPlayer` ve `InfoDetailRow` aynı dosyada. | Görsel değişiklik riskli ve yavaş |
| Player kopyası | `Screens.kt` içinde yerel `FullScreenPlayer` + `FullScreenPlayer.kt` içinde aynı imzayla ikinci kopya. | İki farklı player’ı senkron tutma riski |
| Compose sürümü | BOM `2024.09.00`; güncel kararlı sürüm `2026.09.00`. | Yeni motion/shared-element API’lerinden mahrum |
| Navigasyon | Navigation Compose yok; `selectedTab` + `AnimatedContent` + `AnimatedVisibility` ile manuel yönetim. | Shared-element geçişleri için özel çözüm gerekiyor |
| Hareket dili | Ağırlıklı olarak `tween(300–500ms)`; ortak süre/easing/spring tokenı yok. | Animasyonlar tutarsız, bazen ağır |
| Tipografi | Varsayılan Material tipografisi; özel display font yok, letter-spacing elle veriliyor. | Marka kişiliği zayıf |
| Yüzeyler | “Cam” efekti yarı saydam `Surface` + border ile taklit ediliyor; gerçek blur/RenderEffect yok. | Derinlik hissi sınırlı |
| Ana sayfa | Statik gradyan hero + aynı ağırlıkta kartlar; parallax, katman, editorial hiyerarşi yok. | İlk ekran “özel” hissettirmiyor |
| Player | 260 dp kapak + 6 ikonlu toolbar + volume + visualizer + seek + kontrol: dikey sıkışma. | Küçük ekranlarda taşma/nefes alamama |
| Mini player | Tam ekran player’a sadece slide ile geçiyor; kapak sürekliliği yok. | “Wow” anı kaçırılıyor |
| Kütüphane | Arama yalnız kütüphane sekmesinde; A-Z hızlı kaydırma, swipe aksiyonları, yoğunluk seçimi yok. | Uzun listelerde kullanım zorluğu |
| Boş durumlar | Tek ikon + metin. | Marka deneyimi kopukluğu |
| Erişilebilirlik | `contentDescription` eksikleri, reduced-motion desteği yok, kontrast değişken. | Erişilebilirlik ve kalite riski |

### 1.3 Teknik borç → görsel iş öncesi çözülmeli

1. `Screens.kt` dosyasını feature paketlerine ayır (`ui/home`, `ui/library`, `ui/playlists`, `ui/player`, `ui/settings`).
2. İki `FullScreenPlayer` tanımından tek birini bırak; tercihen `ui/player/FullScreenPlayer.kt`.
3. Tema kaynağını tekilleştir: `ui/theme` altında renk, tipografi, şekil ve motion tokenları.
4. Compose BOM’u kademeli olarak `2026.09.00`’a yükselt; önce ayrı branch’te derleme + test.
5. Görsel regresyon için başlangıç ekran görüntülerini al (aşağıdaki cihaz matrisi).

---

## 2. Vizyon: “Aurora Glass”

### 2.1 Tasarım ilkeleri

1. **Albüm sahnedir.** Renk, ışık ve atmosfer çalan şarkıdan gelir; arayüz onun etrafında nefes alır.
2. **Cam, ama buzlu cam.** Yarı saydamlık + ince ışık kenarı + yumuşak gölge; ağır blur yok.
3. **Az ama büyük.** Her ekranda tek bir kahraman öğe; geri kalanı sakin destekleyici.
4. **Hareket bir anlatıdır.** Ekranlar kaybolmaz; dönüşür. Kapak mini player’dan büyüyerek sahneye çıkar.
5. **Sessiz lüks.** Neon bağırmaz; koyu tuval üzerinde seçici parıltı kullanılır.
6. **Dokunmak keyifli.** Her basışta yay hissi, her eşik geçişinde hafif dokunsal geri bildirim.

### 2.2 Alternatif yönler (karar için)

| Yön | Karakter | Risk |
|---|---|---|
| **Aurora Glass (önerilen)** | Sinematik, koyu, akışkan, albüm-reactive | Blur performansı dikkat ister |
| Neo-Brutal / Editorial | Cesur tipografi, yüksek kontrast, poster estetiği | Mevcut cam bileşenlerle uyumsuz |
| Soft Organic / Clay | Pastel, yuvarlak, oyuncul, yumuşak gölgeler | Müzikçalar için “premium” algısı zayıf olabilir |

---

## 3. Tasarım Sistemi Temelleri

### 3.1 Renk

```text
AuroraBlack     #07080F   ana tuval
AuroraSurface   #10121C   yükseltilmiş yüzey
GlassWhite      %6–10     cam katman
GlassBorder     %14–20    1 dp ışık kenarı
TextPrimary     #F7F8FC
TextSecondary   #A7ADBE
TextTertiary    #6B7185
Accent          Albümden (Palette) — fallback #22D3EE / #A855F7
Success         #4ADE80
Danger          #FB7185
```

Kurallar:
- Ana aksiyon rengi **her zaman albümden** gelir; tema seçimi yalnızca fallback’i değiştirir.
- Metin kontrastı en az **WCAG AA** (4.5:1); dinamik renk `luminance()` ile ayarlanır (mevcut `adjustColorForContrast` korunur).
- AMOLED temasında saf siyah, diğerlerinde `#07080F`.
- Gradyanlar tek yönlü ve düşük doygunlukta; “gökkuşağı” değil, “aurora” hissi.

### 3.2 Tipografi

| Rol | Font | Boyut / ağırlık | Kullanım |
|---|---|---|---|
| Display | **Sora** (SIL OFL) | 32–40 sp / 700 | Splash, hero başlıklar |
| Title | Sora | 22–28 sp / 650 | Ekran başlıkları |
| Body | **Inter** (SIL OFL) | 14–16 sp / 400–500 | Paragraflar, liste |
| Label | Inter | 11–12 sp / 600, +0.4 tracking | Bölüm etiketleri |
| Numeric | Inter tabular | 11–12 sp | Süre, sayaç |

- Türkçe karakter desteği doğrulanacak (ğ, ş, ı, İ, ö, ü, ç).
- Değişken font kullanılırsa APK’ya ~300–500 KB eklenir; iki ayrı statik font yerine tek variable font tercih edilmeli.
- `letterSpacing` yalnızca label/display’de token üzerinden verilir; gövde metninde elle ayar yok.

### 3.3 Şekil ve yüzey hiyerarşisi

| Katman | Kullanım | Özellik |
|---|---|---|
| Canvas | Ekran arka planı | Aurora gradient + hafif grain |
| Glass | Mini player, nav, kontrol dock | %8 beyaz, 1 dp gradient border, 24–32 dp radius |
| Raised | Kartlar, liste öğeleri | %12 beyaz / surface, 20–24 dp radius, renkli gölge |
| Solid | Bottom sheet, dialog | Opak surface, 28 dp üst radius |
| Focus | Aktif satır/öğe | Primary %10 dolgu + 1.5 dp primary border |

- Köşe ölçeği: **12 / 16 / 20 / 24 / 28 / 32 / full**.
- Gölge yerine **renkli ambient glow**: `shadow(ambientColor = accent.copy(0.25f), spotColor = ...)`.
- İkon boyutu: 20 / 22 / 24 / 28; dokunma hedefi minimum **48 dp**.

### 3.4 Hareket dili

```kotlin
object BeatBoxMotion {
    const val Instant   = 120   // basış, ripple
    const val Quick     = 180   // ikon morph, chip
    const val Standard  = 280   // sayfa geçişi, sheet
    const val Cinematic = 460   // hero morph, splash

    val Soft   = spring(dampingRatio = 0.85f, stiffness = 380f)
    val Snappy = spring(dampingRatio = 0.70f, stiffness = 900f)
    val Bouncy = spring(dampingRatio = 0.55f, stiffness = 500f)
}
```

Kurallar:
- Liste girişlerinde **40 ms stagger**; 8’den fazla öğede stagger kesilir.
- Aynı anda en fazla **iki** hareket katmanı (konum + opacity).
- `AnimatedVisibility` yerine mümkün olduğunda `AnimatedContent` + `SizeTransform`.
- Kullanıcı “hareketi azalt” seçtiyse tüm spring’ler `Instant`/fade’e düşer.
- Dokunsal geri bildirim: nav değişimi, favori, sheet eşiği, player morph tamamlanması.

### 3.5 İkonografi

- Rounded Material ikonları korunur; ancak **tutarlı ağırlık** (22 dp gövde, 24 dp nav).
- Özel ikon gerektiren yerler: logo, waveform, “aurora” çalar, uyku zamanlayıcı halkası.
- Launcher ikonu: mevcut adaptive icon’a **monochrome** katman eklenir (Android 13+ themed icons).

---

## 4. Ekran Ekran Dönüşüm

| Ekran | Bugün | Hedef |
|---|---|---|
| **Splash** | 1.9 s sabit, dönen aura, logo kartı | 1.2 s; logo “kalp atışı” pulse’ı, aurora bloom, progress çizgisi ana sayfaya morph |
| **Onboarding** | Statik ikonlu 3 sayfa | Etkileşimli mini demolar: kaydırma, canlı karaoke örneği, tema seçimi; parallax |
| **Ana sayfa** | Gradyan hero + 3 bölüm | “İyi akşamlar” + devam eden çalma kartı, yatay albüm rafı, sanatçı halkaları, ruh hali çipleri, kaydırmada parallax |
| **Kütüphane** | Tek tip kart listesi | Sticky arama, filtre çipleri, A-Z hızlı kaydırıcı, swipe aksiyonları, yoğunluk seçimi |
| **Çalma listeleri** | Düz kartlar | Mozaik kapaklar, akıllı liste gradyanları, sürükle-bırak sıralama, boş durum illüstrasyonu |
| **Mini player** | Cam kart | Yüzen pill, ilerleme hairline’ı, yukarı kaydırınca morph, yatay kaydırmada parça değişimi |
| **Tam ekran player** | Sıkışık dikey yığın | 3 bölgeli sahne: kapak (40–45%), aurora, waveform seek + yüzen kontrol dock’u; aşağı sürükle kapat |
| **Şarkı sözleri** | Yenilendi | Kelime bazlı spring, çeviri toggle’ı, satır merkezleme yumuşatma, reaktif arka plan |
| **Ayarlar** | Standart liste | Profil hero, gruplanmış cam paneller, canlı tema stüdyosu, spring toggle’lar |
| **Diyaloglar/sheet’ler** | Material varsayılan | Ortak `BeatBoxSheet`: tutamaç, 28 dp üst köşe, cam, spring, blur scrim |

### 4.1 Tam ekran player — yeni düzen

```text
┌─────────────────────────────┐
│  ⌄        ŞİMDİ ÇALIYOR   ⋯ │  ← ince üst bar
│                             │
│      ╭───────────────╮      │
│      │               │      │  ← 40–45% yükseklik, 32 dp radius
│      │   ALBÜM       │      │     parallax + aurora glow
│      │   KAPAĞI      │      │
│      ╰───────────────╯      │
│                             │
│   Şarkı Adı                 │
│   Sanatçı · Albüm           │
│                             │
│   ~~~~ waveform seek ~~~~   │  ← dokununca dalga büyür
│   01:24              03:47  │
│                             │
│   ╭───────────────────────╮ │
│   │  ⇄   ⏮   ▶   ⏭   ↻  │ │  ← yüzen cam dock
│   ╰───────────────────────╯ │
│                             │
│   ♡      💬      ☰      ⋯   │  ← ikincil aksiyonlar
└─────────────────────────────┘
```

- EQ, uyku zamanlayıcı, ses, bilgi ve paylaş **“⋯” araç sayfasına** taşınır; ana ekran sadeleşir.
- Kapak, mini player’dan **shared-element** ile büyür; kapanırken aynı yere döner.
- Aşağı sürükleme eşiği %18; eşik aşılınca hafif haptik + lastik bant.
- Küçük ekran (360×640) ve font scale 1.3 için ayrı önizleme.

### 4.2 Ana sayfa — yeni hiyerarşi

1. **Selamlama + profil avatarlı üst bar** (kaydırmada compact’a dönüşür).
2. **Devam eden çalma** kartı: kapak, ilerleme halkası, “kaldığın yerden” + play.
3. **Senin için** rafı: albüm kapakları, yatay parallax.
4. **Son çalınanlar** rafı: 2×3 grid veya yatay kartlar.
5. **Sanatçılar** halkaları: renkli ring, ilk harf fallback.
6. **Ruh hali / tür** çipleri (veri varsa).
7. **İstatistik kartı**: “Bu hafta 4 saat” gibi küçük, zarif bir özet.

### 4.3 Kütüphane

- Arama çubuğu **tüm sekmelerde** erişilebilir (üst bar’da ikon → genişleyen arama).
- `LazyColumn` üstünde sticky filtre çipleri: Tümü / Şarkılar / Albümler / Sanatçılar / Klasörler.
- Sağ kenarda **A-Z scrubber**; sürüklerken büyük harf balonu.
- Satırda sola kaydır: favori/kuyruk; sağa kaydır: sil (onaylı).
- Uzun basış: çoklu seçim modu, üst bar “3 seçili” + toplu işlem.
- Yoğunluk toggle’ı: rahat (kapak 60 dp) / kompakt (40 dp).

---

## 5. İmza Anları (“Wow List”)

| # | An | Etki | Efor | Not |
|---|---|---|---|---|
| 1 | Mini player → tam ekran **kapak morph’u** | ★★★★★ | Yüksek | Shared element veya özel bounds animasyonu |
| 2 | Albümden doğan **aurora arka plan** | ★★★★★ | Orta | Palette + radial gradient + yavaş drift |
| 3 | **Waveform seek bar** | ★★★★☆ | Orta | Mevcut `visualizerWaveform` verisi |
| 4 | Navigasyon **pill indicator** + ikon morph | ★★★★☆ | Düşük | Haptik ile |
| 5 | **Tema stüdyosu**: canlı mini önizlemeler | ★★★★☆ | Orta | 4 tema + dinamik |
| 6 | **Etkileşimli onboarding** demoları | ★★★★☆ | Orta | İlk izlenim |
| 7 | **Splash → home** logo morph | ★★★☆☆ | Düşük | 1.2 s |
| 8 | Albüm kapağında **parallax tilt** | ★★★☆☆ | Düşük | `graphicsLayer` + sensör/jest |
| 9 | Favori/ekleme **micro-toast + haptik** | ★★★☆☆ | Düşük | Tutarlı geri bildirim |
| 10 | Uyku zamanlayıcı **halka animasyonu** | ★★★☆☆ | Düşük | Ayarlar içinde |

> İlk üç madde, uygulamanın “havaya uçtu” dedirtecek çekirdek anları. Faz 1–3’te önceliklendirilmeli.

---

## 6. Faz Planı

### Faz 0 — Temel ve Temizlik (2–3 gün)

**Amaç:** Görsel işlerin hızlı ve güvenli ilerlemesi için zemini hazırlamak.

İşler:
- [x] Compose BOM `2024.09.00` → `2026.09.00` (ayrı branch, derleme + test). BOM 1.12.1 getirdiği için `compileSdk` 37'ye yükseltildi.
- [x] `ui/theme` altında `Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`, `Theme.kt` tek kaynak.
- [x] `MainActivity.BeatBoxTheme` ile `getThemeColors()` birleştir; `ui/theme/Theme.kt` kullanılmıyorsa kaldır.
- [x] `Screens.kt` bölünmesi: `ui/home`, `ui/library`, `ui/playlists`, `ui/player`, `ui/settings`, `ui/components`.
- [x] İki `FullScreenPlayer` tanımından tekini sil; kullanılanı `ui/player` altına taşı.
- [ ] Ortak bileşen envanteri: `GlassSurface`, `AuroraBackground`, `BeatBoxButton`, `BeatBoxSheet`, `SectionHeader`, `SongRow`.
- [ ] Compose Preview galerisi + screenshot test altyapısı.
- [ ] Başlangıç ekran görüntüleri: 360×640, 411×891, 800×1280.

**Kabul kriterleri:**
- `:app:testDebugUnitTest` 20/20 yeşil.
- `:app:assembleRelease` başarılı; APK’da API anahtarı yok.
- Görsel regresyon yok (mevcut ekranlar birebir aynı).
- `Screens.kt` < 400 satır; her feature dosyası < 800 satır.

---

### Faz 1 — Ambient Kabuk (3–4 gün)

**Amaç:** Uygulamanın “ilk 5 saniyesi” ve genel iskeleti premium hissettirsin.

İşler:
- [x] **Splash 1.2 s**: logo beat pulse, aurora bloom, progress line; cold start uzarsa determinate.
- [x] **AuroraBackground** bileşeni: albüm renginden 2 radial gradient + yavaş drift + grain.
- [x] **Alt navigasyon**: pill indicator, ikon morph, seçimde haptik; cam yüzey.
- [x] **Mini player**: yüzen pill, kapak, ilerleme hairline, play/pause morph, yukarı kaydırma ipucu.
- [x] **Sayfa geçişleri**: fade+slide yerine `AnimatedContent` + `SizeTransform`; sekme yönü korunur.
- [x] **Skeleton loading**: kütüphane ve ana sayfa için parıldayan iskelet.
- [x] **Edge-to-edge** + status/nav bar ikon renkleri tema ile uyumlu.
- [x] Reduced-motion temeli.

**Kabul kriterleri:**
- Soğuk açılış < 1.5 s (Pixel 6a referans).
- Geçişlerde takılma yok; 60 fps (mid-range), 120 Hz’de akıcı.
- Navigasyon durumu TalkBack ile okunuyor.
- Mini player tüm sekmelerde aynı görünüyor.

> Cihaz/emülatör bulunmadığı için soğuk açılış, 60 fps ve TalkBack
> doğrulamaları henüz saha testi bekliyor. Kod tarafında splash 1.2 s'ye
> sabitlendi; navigasyon `Role.Tab` + `selected` semantiği kullanıyor.

---

### Faz 2 — Ana Sayfa, Kütüphane, Çalma Listeleri (4–5 gün)

**Amaç:** Uygulamanın en çok görülen ekranlarını “editorial” bir deneyime dönüştürmek.

İşler:
- [ ] Ana sayfa yeni hiyerarşi (selamlama, devam eden çalma, raflar, sanatçı halkaları).
- [ ] Kaydırmada parallax + compact üst bar.
- [ ] Albüm/sanatçı kartlarında renkli gölge ve basış spring’i.
- [ ] Kütüphane: sticky arama, filtre çipleri, A-Z scrubber, swipe aksiyonları, çoklu seçim.
- [ ] Çalma listeleri: mozaik kapak, akıllı liste gradyanları, boş durum illüstrasyonu.
- [ ] `SongRow` tek bileşen: kütüphane, çalma listesi, sıra ve arama sonuçlarında aynı.
- [ ] Boş durumlar için 4–6 illüstrasyon (vektör).

**Kabul kriterleri:**
- 1.000 şarkılı kütüphanede kaydırma 60 fps.
- Arama sonuçları < 100 ms güncelleniyor.
- Tüm satır aksiyonları 48 dp hedefte.
- Ekran görüntüsünde ilk bakışta tek bir kahraman öğe var.

---

### Faz 3 — Player ve Şarkı Sözleri (4–5 gün)

**Amaç:** Ürünün kalbi olan çalma deneyimini sahneye dönüştürmek.

İşler:
- [ ] Mini → full **kapak morph’u** (shared element veya özel bounds).
- [ ] Yeni 3 bölgeli player düzeni; araçları “⋯” sayfasına taşı.
- [ ] Waveform seek bar; dokununca genişleme, sürüklerken zaman balonu.
- [ ] Aşağı sürükle kapat + lastik bant + haptik.
- [ ] Arka planda aurora, kapakta parallax.
- [ ] Kuyruk sheet’i: sürükle-bırak, “şimdi çalıyor” vurgusu.
- [ ] Şarkı sözleri: kelime spring’i, satır merkezleme yumuşatma, çeviri toggle’ı.
- [ ] Uyku zamanlayıcı ve EQ için kompakt, görsel araç kartları.

**Kabul kriterleri:**
- Morph 60 fps; geri kapanışta kapak doğru konuma dönüyor.
- 360×640 ekranda hiçbir kontrol üst üste binmiyor.
- Player açıkken pil tüketimi mevcut seviyeyi aşmıyor.
- Kuyruk ve söz geçişleri kesintisiz.

---

### Faz 4 — Delight ve Detaylar (2–3 gün)

İşler:
- [ ] Tüm dokunulabilir yüzeylerde basış spring’i + haptik.
- [ ] Tema stüdyosu: 4 tema + dinamik, canlı mini önizleme kartları.
- [ ] Etkileşimli onboarding: kaydırma, karaoke, tema demosu.
- [ ] Favori, indirme, ekleme için micro-toast ve ikon morph.
- [ ] Uyku zamanlayıcı halkası; EQ için canlı spektrum.
- [ ] “Paylaş” kartı: albüm kapağı + şarkı bilgisi ile görsel üretimi (stretch).
- [ ] Launcher monochrome katman.

**Kabul kriterleri:**
- Onboarding’i tamamlayan kullanıcı oranı ölçülebilir (analytics varsa).
- Tema değişimi anında, sayfa yeniden yüklenmeden.
- Tüm mikro-etkileşimler aynı süre/spring ailesinden.

---

### Faz 5 — Performans, Erişilebilirlik, Release (2–3 gün)

İşler:
- [ ] Baseline Profile + Macrobenchmark; açılış ve kaydırma ölçümleri.
- [ ] `drawWithCache`, `derivedStateOf`, `Immutable` listeler, gereksiz recomposition temizliği.
- [ ] Blur/RenderEffect yalnız API 31+; önceden küçültülmüş bitmap fallback.
- [ ] Kontrast denetimi (AA), TalkBack etiketleri, 48 dp hedefler, font scale 1.3.
- [ ] Reduced-motion tüm animasyonlarda.
- [ ] Screenshot testleri + cihaz matrisi.
- [ ] Release APK doğrulaması: anahtar yok, R8 kuralları, çökme yok.

**Kabul kriterleri:**
- Açılış < 1.5 s; geçişlerde 16 ms üstü frame yok (mid-range).
- TalkBack ile tüm ana akış tamamlanabiliyor.
- 0 yeni çökme; 20/20 test; lint temiz.
- APK boyutu artışı < 1.5 MB.

---

## 7. Öncelik Matrisi

| Etki / Efor | Düşük efor | Yüksek efor |
|---|---|---|
| **Yüksek etki** | Navigasyon pill + haptik, splash 1.2 s, basış spring’leri, skeleton, boş durumlar, mini player cilası | Kapak morph’u, aurora arka plan, player yeniden düzeni, waveform seek |
| **Orta etki** | Tema stüdyosu, monochrome ikon, micro-toast | Etkileşimli onboarding, paylaşım kartı, A-Z scrubber |
| **Düşük etki** | Grain dokusu, ikon ağırlığı | Tam ekran “wrapped” deneyimi, 3D kapak |

**İlk 10 gün için öneri:** Navigasyon pill + splash + basış spring’leri + mini player + aurora arka plan + ana sayfa hero. Bu altı iş, algılanan kaliteyi en hızlı yükseltir.

---

## 8. Teknik Uygulama Notları

### 8.1 Compose BOM yükseltmesi

- Hedef: `2026.09.00`.
- Riskler: Material3 API değişiklikleri, `ExperimentalMaterial3Api` kullanımları, `TopAppBar` renkleri.
- Strateji: ayrı branch → `compileDebugKotlin` → testler → lint → release → görsel karşılaştırma.
- Yeni API’ler: `SharedTransitionLayout`, `Modifier.sharedElement`, `Modifier.animateBounds`, `LookaheadScope`, `graphicsLayer` ile daha ucuz animasyonlar.

### 8.2 Shared element olmadan morph alternatifi

Uygulama Navigation Compose kullanmadığı için shared element geçişi manuel kurgulanmalı. İki seçenek:

1. **SharedTransitionLayout**: kökü sar, mini player kapağına ve full player kapağına aynı key’i ver. `AnimatedVisibilityScope` gerekir; mevcut `AnimatedVisibility` yapısıyla uyumlu hale getirilir.
2. **Özel hero morph**: mini player kapak bounds’unu `onGloballyPositioned` ile ölç; tam ekran açılırken `Animatable` ile konum/boyut/radius interpolasyonu yap. Daha fazla kod, ama tam kontrol ve düşük risk.

Öneri: önce (1) dene; küçük ekran/geri tuşu kenar durumlarında sorun çıkarsa (2)’ye geç.

### 8.3 Aurora arka plan performansı

- Albüm kapağını **64×64**’e küçült, `Palette` ile 2 renk çıkar.
- `Canvas` + `drawWithCache` ile 2 radial gradient; offset’leri `infiniteTransition` ile yavaşça kaydır.
- `Modifier.blur`’u tam ekran bitmap üzerinde her frame çalıştırma; önceden blur’lanmış küçük bitmap kullan.
- `RenderEffect` yalnız API 31+; altında yarı saydam gradient fallback.
- Performans modunda aurora drift’i durdur.

### 8.4 Waveform seek

- `visualizerWaveform` zaten 15 örnekli; seek bar için 48–64 örneklik geçmiş tampon tut.
- Çalma sırasında `Canvas` üzerinde çiz; her frame recomposition değil, `State<FloatArray>` + `drawBehind`.
- Sürükleme sırasında zaman balonu; bırakınca `seekTo`.

### 8.5 Dosya/bileşen hedef yapısı

```text
com.blackstack.beatbox
├── ui/theme/        Color.kt, Type.kt, Shape.kt, Motion.kt, Theme.kt
├── ui/components/   GlassSurface, AuroraBackground, BeatBoxSheet, SongRow, SectionHeader
├── ui/home/         HomeScreen.kt
├── ui/library/      LibraryScreen.kt, SearchBar.kt
├── ui/playlists/    PlaylistsScreen.kt, PlaylistDetail.kt
├── ui/player/       FullScreenPlayer.kt, MiniPlayer.kt, QueueSheet.kt
├── ui/lyrics/       LyricsScreen.kt
├── ui/settings/     SettingsScreen.kt, ThemeStudio.kt
└── ui/onboarding/   SplashScreen.kt, OnboardingScreen.kt
```

### 8.6 Test ve doğrulama

- Unit: mevcut 20 Kotest testi korunur; yeni saf fonksiyonlar (renk kontrastı, süre biçimi, waveform örnekleme) için test eklenir.
- Screenshot: Compose Preview Screenshot Testing (AGP 9.4) veya Paparazzi.
- Cihaz matrisi: 360×640 (küçük), 411×891 (Pixel), 800×1280 (tablet), font scale 1.3, TR/EN/AR.
- Release: `assembleRelease` + dex kontrolü (API anahtarı yok, FlowRow yok).

---

## 9. Erişilebilirlik ve Kalite Çubuğu

- Tüm ikon butonlarında `contentDescription`; dekoratif ikonlarda `null`.
- Dokunma hedefi ≥ 48 dp; küçük ikonlarda görünmez padding.
- Renk tek başına anlam taşımasın: aktif sekme + etiket, favori + dolu ikon.
- Kontrast: gövde metni 4.5:1, büyük başlık 3:1.
- `LocalAccessibilityManager` ile reduced motion; `Settings.Global.ANIMATOR_DURATION_SCALE` desteği.
- TalkBack sırası: başlık → kahraman içerik → kontroller.
- Font scale 1.3’te taşma yok; `maxLines` + ellipsis.

---

## 10. Riskler ve Önlemler

| Risk | Olasılık | Etki | Önlem |
|---|---|---|---|
| Compose BOM yükseltmesi derlemeyi bozar | Orta | Yüksek | Ayrı branch, kademeli yükseltme, görsel regresyon |
| Blur/aurora performansı düşürür | Orta | Yüksek | Küçük bitmap, `drawWithCache`, performans modu |
| Shared element küçük ekranda kayar | Orta | Orta | Özel bounds morph fallback |
| `Screens.kt` bölünmesi sırasında regresyon | Yüksek | Orta | Önce mekanik taşıma, sonra tasarım; testler |
| Türkçe metinler taşar | Orta | Orta | 360 dp + font scale 1.3 önizleme |
| İki player kopyası farklılaşır | Yüksek | Orta | Faz 0’da tek kaynak |
| Tema başına renk kontrastı bozulur | Orta | Orta | `adjustColorForContrast` + otomatik kontrast testi |

---

## 11. Ölçüm ve Başarı Kriterleri

**Algısal:**
- 5 kişilik iç test: “Bu uygulama premium hissettiriyor” ≥ 4.5/5.
- İlk açılışta 3 saniye içinde “wow” anı: splash → aurora → hero kart.

**Teknik:**
- Soğuk açılış < 1.5 s.
- Kaydırma/geçişlerde 16 ms üstü frame oranı < %1 (mid-range).
- APK artışı < 1.5 MB.
- 0 yeni çökme; 20/20 test; lint temiz.

**Erişilebilirlik:**
- TalkBack ile ana akış %100 tamamlanabilir.
- Tüm metinler AA kontrast.
- Reduced motion açıkken animasyon süreleri ≤ 120 ms.

---

## 12. İhtiyaç Duyulan Varlıklar ve Kararlar

**Varlıklar:**
- [ ] Sora + Inter variable font (lisans kontrolü).
- [ ] 4–6 boş durum illüstrasyonu (SVG/vektör).
- [ ] Grain dokusu (tek PNG, tileable, < 50 KB).
- [ ] Monochrome launcher ikonu.
- [ ] Paylaşım kartı şablonu (opsiyonel).

**Kararlar:**
1. Yön: **Aurora Glass** mi, alternatif mi?
2. Font: **Sora + Inter** mi, yoksa yalnız sistem fontu mu?
3. Kapsam: yalnız ana 3 ekran mı, tüm uygulama mı? (Öneri: tümü.)
4. Dinamik renk: her zaman albümden mi, tema seçimi baskın mı? (Öneri: albümden, tema fallback.)
5. Minimum hedef: 60 fps mid-range mi, 120 Hz amiral gemisi mi? (Öneri: 60 fps mid-range + 120 Hz’de akıcı.)

---

## 13. Önerilen Başlangıç

**Bugün başlanacaksa:**

1. `docs/VISUAL_ROADMAP.md` onayı.
2. Faz 0 için branch: `feat/visual-foundation`.
3. Compose BOM yükseltmesi + tema birleştirme + `Screens.kt` bölünmesi.
4. Faz 1: splash, navigasyon, mini player, aurora arka plan.
5. İlk görsel demo: ana sayfa + mini player + tam ekran player morph.

**Toplam hedef:** 3 hafta içinde “Aurora Glass” deneyiminin tüm ana ekranlarda çalışır halde olması.

---

> Bu doküman canlı bir plan: her faz sonunda ekran görüntüleriyle güncellenecek, kabul kriterleri sağlanmadan bir sonraki faza geçilmeyecek.
