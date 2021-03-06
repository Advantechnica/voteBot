/*
 * Votebot - A feature-rich bot to create votes on Discord guilds.
 *
 * Copyright (C) 2019  Michael Rittmeister & Yannick Seeger & Daniel Scherf
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

package wtf.votebot.bot.core

import com.google.common.flogger.FluentLogger
import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.model.agent.Registration

/**
 * ServiceRegistry handles the the registration of this service in consul.
 */
class ServiceRegistry(private val serviceName: String, httpPort: String) {

    private val log = FluentLogger.forEnclosingClass()
    private val client: Consul = Consul.builder().build()
    private val agentClient: AgentClient

    init {
        agentClient = client.agentClient()
        val service = ImmutableRegistration.builder()
            .id(serviceName)
            .name(serviceName)
            .check(Registration.RegCheck.ttl(3))
            .check(Registration.RegCheck.http("http://localhost:$httpPort", 5L))
            .build()
        agentClient.register(service)
        agentClient.pass(serviceName)
        log.atInfo().log("Registered service as: `%s`", serviceName)
    }

    /**
     * Unregisters the service from Consul.
     */
    fun deregister() {
        log.atInfo().log("Deregistering service from Service Registry.")
        agentClient.deregister(serviceName)
        log.atInfo().log("Deregistered service from Service Registry.")
    }
}
