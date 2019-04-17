/*
 * VoteBot - A unique Discord bot for surveys
 *
 * Copyright (C) 2019  Michael Rittmeister
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package me.schlaubi.votebot.core.graphics

import java.text.DecimalFormat

/**
 * Data class containing [title] and [percentage]
 */
data class PieTile(
    var title: String,
    val percentage: Double
) {
    init {
        // Cut off heading when name is too long
        val name = if (title.length >= 10) title.substring(0, 10) + "..." else title
        // Format percentage and title
        this.title = "$name - ${DecimalFormat("##.#").format(percentage * 100)}%"
    }
}
