package com.blackstack.beatbox

import com.blackstack.beatbox.ui.home.greetingKeyForHour
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Ana sayfa selamlamasının saat sınırlarını doğrular.
 */
class HomeGreetingTest : StringSpec({

    "morning starts at 05:00" {
        greetingKeyForHour(4) shouldBe "good_evening"
        greetingKeyForHour(5) shouldBe "good_morning"
        greetingKeyForHour(11) shouldBe "good_morning"
    }

    "afternoon covers 12:00 to 17:59" {
        greetingKeyForHour(12) shouldBe "good_afternoon"
        greetingKeyForHour(17) shouldBe "good_afternoon"
    }

    "evening covers the remaining hours" {
        greetingKeyForHour(18) shouldBe "good_evening"
        greetingKeyForHour(23) shouldBe "good_evening"
        greetingKeyForHour(0) shouldBe "good_evening"
    }
})
