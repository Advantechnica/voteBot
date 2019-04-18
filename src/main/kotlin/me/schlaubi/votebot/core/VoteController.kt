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

package me.schlaubi.votebot.core

import cc.hawkbot.regnum.client.util.TranslationUtil
import me.schlaubi.votebot.core.graphics.PieChart
import me.schlaubi.votebot.core.graphics.PieTile
import me.schlaubi.votebot.entities.Vote
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer
import java.util.function.Function

class VoteController(
    private val vote: Vote
) {

    init {
        vote.initialize()
    }

    private fun updateMessages(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        vote.messages
                // Map to messages only
            .thenApplyAsync(Function<Map<Message, TextChannel>, Set<Message>> { t -> t.keys }, VoteCache.THREAD_POOL)
                // Edit messages
            .thenApplyAsync(
                Function<Set<Message>, List<CompletableFuture<Message>>>
                { it.map { message -> message.editMessage(vote.renderVote().build()).submit() } },
                VoteCache.THREAD_POOL
            )
                // Map futures
            .thenAcceptAsync(Consumer {
                CompletableFuture.allOf(*it.toTypedArray())
                    .thenRunAsync(Runnable {
                        future.complete(null)
                    }, VoteCache.THREAD_POOL)
            }, VoteCache.THREAD_POOL)
        return future
    }

    fun addVote(user: User, answer: Int): CompletionStage<Void> {
        // Check if option does exist
        if (answer >= vote.options.size) {
            throw IllegalArgumentException("This option does not exist")
        }

        // Check if user has already voted for the same option
        val maximumVotes = vote.maximumVotes
        val userId = user.idLong
        val userVotes = vote.answers[userId]
        if (userVotes != null && answer in userVotes) {
            throw IllegalArgumentException("User already voted for that")
        }
        // Check for "opinion changeable" mode or "multiple opinon" mode
        if (maximumVotes == 1) {
            // Check if user is allowed to vote
            val maximumChanges = vote.maximumChanges
            var voteCount = vote.voteCounts[userId] ?: 0
            if (voteCount >= maximumChanges) {
                throw IllegalStateException("To may votes")
            }
            // Register vote
            vote.answers[userId] = mutableListOf(answer)
            voteCount++
            vote.voteCounts[userId] = voteCount
        } else {
            // Check if user has already storage to much.
            var votes = vote.answers[userId]
            if (votes != null) {
                if (votes.size == maximumVotes) {
                    throw IllegalStateException("You have votes to much!")
                }
                votes.add(answer)
            } else {
                votes = mutableListOf(answer)
            }
            // Save the result
            vote.answers[userId] = votes
        }
        // Update all messages and save vote -> combine it into one future
        return CompletableFuture.allOf(vote.saveAsync().toCompletableFuture(), updateMessages())
    }

    fun deleteVote() {
        vote.deleteAsync()
        // Only generate chart when it's possible
        if (vote.options.size == vote.options.distinct().size) {
            // Map options to pie tiles
            var tiles = arrayOf<PieTile>()
            var allVotes = 0
            vote.answers.values.forEach {
                allVotes += it.size
            }
            for (i in 0 until vote.options.size) {
                val title = vote.options[i]
                // Counts answers for option
                val voteCount = vote.answers.values.asSequence().filter { chosen -> chosen.contains(i) }.count()
                // Calculate percentage of this option
                val percentage = voteCount.toDouble() / allVotes.toDouble()
                tiles += PieTile(title, percentage)
            }
            val chart: PieChart
            // Generate chart
            chart = PieChart(vote.heading, tiles)
            // Loop through all messages
            vote.messages.thenAccept {
                val messages = it.keys
                // Filter out channels with no permissions
                val channels = it.filterValues { channel ->
                    channel.guild.selfMember.hasPermission(
                        channel,
                        Permission.MESSAGE_ATTACH_FILES
                    )
                }
                if (channels.isEmpty()) {
                    // Fallback handling if no suitable channel could be found
                    fallbackEdit(messages)
                } else {
                    // Send chart to all applicable channels
                    channels.forEach { entry ->
                        entry.value.sendFile(chart.toInputStream(), "chart.png").queue()
                    }
                }
            }
        } else {
            // Fallback edit if chart could not be generated because of dupes
            vote.messages.thenApply { it.keys }
                .thenAccept {
                    fallbackEdit(it)
                }
            throw IllegalArgumentException("Dupes")
        }
    }

    // just edit the footer
    private fun fallbackEdit(messages: MutableSet<Message>) {
        messages.forEach {
            it.editMessage(vote.renderVote().setFooter("Vote closed!", null).build()).queue()
        }
    }


    fun translate(key: String): String {
        return TranslationUtil.translate(vote.cache.bot.regnum, key, vote.authorId)
    }
}