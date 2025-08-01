package com.example.bitchat

import java.util.Locale

object NicknameGenerator {
    private val adjectives = listOf(
        "Swift",
        "Silent",
        "Bright",
        "Clever",
        "Brave",
        "Fuzzy",
        "Lucky",
        "Wild",
        "Sly",
        "Merry",
        "Quick",
        "Nimble",
        "Bold",
        "Glowing",
        "Happy",
        "Jolly",
        "Mighty",
        "Proud",
        "Rugged",
        "Sharp",
        "Sneaky",
        "Stormy",
        "Witty",
        "Zesty",
        "Chilly",
        "Daring",
        "Eager",
        "Gentle",
    )
    private val animals = listOf(
        "Fox",
        "Otter",
        "Hawk",
        "Wolf",
        "Tiger",
        "Panda",
        "Lynx",
        "Dolphin",
        "Eagle",
        "Bear",
        "Rabbit",
        "Shark",
        "Falcon",
        "Jaguar",
        "Koala",
        "Leopard",
        "Moose",
        "Narwhal",
        "Owl",
        "Penguin",
        "Quail",
        "Raccoon",
        "Seal",
        "Turtle",
        "Viper",
        "Walrus",
        "Yak",
        "Zebra",
    )

    fun generate(peerId: String): String {
        val hash = peerId.hashCode()
        val adj = adjectives[(hash shr 3).absoluteValue % adjectives.size]
        val animal = animals[(hash shr 7).absoluteValue % animals.size]
        return "$adj$animal"
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
