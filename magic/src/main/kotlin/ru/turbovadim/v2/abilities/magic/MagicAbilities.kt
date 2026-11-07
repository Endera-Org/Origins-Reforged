package ru.turbovadim.v2.abilities.magic

import ru.turbovadim.v2.ability.Ability

/**
 * Magic Origins module ability collection.
 *
 * Port of the Origins-Magic plugin (StarshooterCity) onto the v2 DSL system.
 *
 * Abilities are grouped by the origin they primarily belong to:
 *  - Alchemist    (Potion Master, Alchemy)
 *  - Healer       (Final Shout, Healing Touch, Healing Focus)
 *  - Hypnotist    (Hypnosis, Confusion)
 *  - Necromancer  (Lord of the Dead, Resurrection Spell, Spirit Strength, Undead Ally, Dark Aura)
 *  - Shadowmancer (Shadow Form, Dark Strength, Fire Weakness, Creature of Darkness, No Fire Resistance)
 *  - Spirit       (Spectral, Ghostly)
 *  - Telekinetic  (Telekinetic Reach, Telekinesis)
 *  - Warlock      (Dark Magic, Cursed Power, Magic Resistant)
 *  - Common       (Unskilled, Hydrophobia) - shared drawbacks
 *
 * Each group is exported as its own list, and [all] holds everything in
 * registration order. [byKey] offers O(1) lookup by namespaced key string.
 */
object MagicAbilities {

    val alchemist: List<Ability> = alchemistAbilities
    val healer: List<Ability> = healerAbilities
    val hypnotist: List<Ability> = hypnotistAbilities
    val necromancer: List<Ability> = necromancerAbilities
    val shadowmancer: List<Ability> = shadowmancerAbilities
    val spirit: List<Ability> = spiritAbilities
    val telekinetic: List<Ability> = telekineticAbilities
    val warlock: List<Ability> = warlockAbilities
    val common: List<Ability> = commonAbilities

    /**
     * Every Magic-module ability in registration order.
     */
    val all: List<Ability> = buildList {
        addAll(common)
        addAll(alchemist)
        addAll(healer)
        addAll(hypnotist)
        addAll(necromancer)
        addAll(shadowmancer)
        addAll(spirit)
        addAll(telekinetic)
        addAll(warlock)
    }

    val byKey: Map<String, Ability> by lazy { all.associateBy { it.key.toString() } }

    fun get(key: String): Ability? = byKey[key]
}
