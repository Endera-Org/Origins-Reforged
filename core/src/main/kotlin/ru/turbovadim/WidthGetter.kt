package ru.turbovadim

object WidthGetter {

    fun getWidth(character: Char): Int {
        if (character == '\uf00a') {
            return 2
        }
        if (character == ' ') {
            return 4
        }
        for (i in 2..16) {
            if (OriginsReforged.charactersConfig.characterWidths[i]?.contains(character) ?: false) {
                return i
            }
        }
        return 0
    }
}