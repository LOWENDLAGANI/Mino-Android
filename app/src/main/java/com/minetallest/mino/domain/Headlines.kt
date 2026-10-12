package com.minetallest.mino.domain

/**
 * Rotating empty-state headlines (spec §6.6): walked in order with a persisted cursor
 * (`mino:empty-headline-cursor`) so consecutive loads never repeat.
 */
object Headlines {
    val all: List<String> = listOf(
        "What's on your mind?",
        "Ask me anything.",
        "I'm listening.",
        "What can we figure out today?",
        "Got questions? I've got answers.",
        "Let's think out loud.",
        "What should we explore?",
        "Your private assistant, ready.",
        "What are we building today?",
        "Curious about something?",
        "Start with anything.",
        "No question is too small.",
        "Let's make something useful.",
        "What do you want to know?",
        "Thinking caps on.",
        "Ready when you are.",
        "What's the first thing on the list?",
        "Let's untangle something complicated.",
        "What are we solving?",
        "A blank page, and no audience.",
        "What would make today easier?",
        "Let's dig in.",
        "You bring the question.",
        "What's next on your mind?",
        "Let's get into it.",
        "What's something you've been wondering?",
        "Fresh session, clear head.",
        "What shall we look at first?",
        "Tell me where to start.",
        "What are you curious about today?",
        "Let's write the first line together.",
        "Something on your mind?",
        "What's worth a few good minutes?",
        "Let's chase that idea.",
        "What do you need?",
        "Ready for the next thing.",
        "What are we thinking about?",
        "Let's figure it out.",
        "Ask away.",
        "What's the mission?",
    )

    /** Next index + text; cursor persists after display. */
    fun next(cursor: Int): Pair<Int, String> {
        val index = ((cursor % all.size) + all.size) % all.size
        return index to all[index]
    }
}
