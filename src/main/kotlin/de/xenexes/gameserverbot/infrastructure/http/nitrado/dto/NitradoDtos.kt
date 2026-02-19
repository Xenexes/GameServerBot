package de.xenexes.gameserverbot.infrastructure.http.nitrado.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Handles Nitrado's inconsistent `has_steam_game` field which can be a boolean or the string "unknown".
 * Maps any non-"true" value to false.
 */
object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && !element.isString -> element.content.lowercase() == "true"
            element is JsonPrimitive -> element.content.lowercase() == "true"
            else -> false
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
data class NitradoApiResponse<T>(
    val status: String,
    val data: T? = null,
    val message: String? = null,
)

@Serializable
data class NitradoGameServerData(
    val gameserver: NitradoGameServer,
)

@Serializable
data class NitradoGameServer(
    val status: String,
    val ip: String,
    val port: Int,
    val game: String,
    @SerialName("game_human")
    val gameHuman: String,
    val slots: Int,
    val location: String,
    @SerialName("service_id")
    val serviceId: Long,
    @SerialName("query_port")
    val queryPort: Int? = null,
    @SerialName("rcon_port")
    val rconPort: Int? = null,
    val memory: String? = null,
    @SerialName("memory_mb")
    val memoryMb: Int? = null,
    val query: NitradoQuery? = null,
)

@Serializable
data class NitradoQuery(
    @SerialName("server_name")
    val serverName: String? = null,
    @SerialName("connect_ip")
    val connectIp: String? = null,
    val map: String? = null,
    val version: String? = null,
    @SerialName("player_current")
    val playerCurrent: Int? = null,
    @SerialName("player_max")
    val playerMax: Int? = null,
)

@Serializable
data class NitradoActionResponse(
    val status: String,
    val message: String? = null,
)

@Serializable
data class NitradoServicesData(
    val services: List<NitradoServiceDto>,
)

@Serializable
data class NitradoServiceDto(
    val id: Long,
    val status: String,
    @SerialName("type_human")
    val typeHuman: String,
    val details: NitradoServiceDetailsDto,
)

@Serializable
data class NitradoServiceDetailsDto(
    val address: String,
    val name: String,
    val game: String,
    val slots: Int,
)

@Serializable
data class NitradoGamesData(
    val games: List<NitradoGameDto> = emptyList(),
)

@Serializable
data class NitradoGameDto(
    val id: String,
    @SerialName("folder_short")
    val folderShort: String,
    @SerialName("steam_id")
    val steamId: String? = null,
    @SerialName("has_steam_game")
    @Serializable(with = FlexibleBooleanSerializer::class)
    val hasSteamGame: Boolean = false,
    val name: String,
    val active: Boolean = false,
    val installed: Boolean = false,
)

@Serializable
data class NitradoPlayersData(
    val players: List<NitradoPlayerDto>,
)

@Serializable
data class NitradoPlayerDto(
    val name: String,
    @SerialName("id")
    val id: String? = null,
    @SerialName("id_type")
    val idType: String? = null,
    val online: Boolean = true,
)

@Serializable
data class NitradoWhitelistData(
    val whitelist: List<NitradoPlayerListEntry>,
)

@Serializable
data class NitradoBanlistData(
    val banlist: List<NitradoPlayerListEntry>,
)

@Serializable
data class NitradoPlayerListEntry(
    val name: String,
    val id: String,
    @SerialName("id_type")
    val idType: String,
)
