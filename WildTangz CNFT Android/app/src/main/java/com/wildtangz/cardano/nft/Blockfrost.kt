package com.wildtangz.cardano.nft

import android.content.Context
import org.apache.commons.codec.binary.Hex
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class Blockfrost(context: Context) {

    companion object {

        const val QUANTITY_KEY : String = "quantity"
        const val TOKEN_KEY : String = "unit"

        private const val STAKE_ADDRESS_KEY : String = "stake_address"
        private const val STAKE_PREFIX : String = "stake1"

        private const val HANDLE_PREFIX : String = "$"
        private const val HANDLE_POLICY_ID : String = "f0ff48bbb7bbe9d59a40f1ce90e9e9d0ff5002ec48f232b49ca0fb9a"

        private const val PAGE_MAX : Int = 100

        private const val IPFS_PROTOCOL : String = "ipfs://"
        private const val IPFS_V0_START : String = "Q"
        private const val IPFS_V1_START : String = "baf"

        private val BLOCKFROST_CARDANO_PREFIX : String =
            "https://cardano-mainnet.blockfrost.io/api/v0"
        private val BLOCKFROST_IPFS_PREFIX : String =
            "https://ipfs.blockfrost.io/api/v0/ipfs/gateway"
        private val BLOCKFROST_CONTENT_TYPE : String = "application/json"
    }

    var cardanoKey : String
    var ipfsKey : String

    init {
        cardanoKey = context.getString(R.string.blockfrost_cardano_key)
        ipfsKey = context.getString(R.string.blockfrost_ipfs_key)
    }

    fun numPolicyTokens(assetOrAddress: String, policy: String) : Int {
        val ownedTokens = getAddressTokens(getWalletName(assetOrAddress))
        var numFound = 0
        for (index in 0 until ownedTokens.length()) {
            if (ownedTokens.getJSONObject(index).getString(TOKEN_KEY).startsWith(policy)) {
                numFound++
            }
        }
        return numFound
    }

    fun getWalletName(addressOrAsset: String): String {
        when (addressOrAsset.startsWith(PoolPm.ASSET_PREFIX)) {
            true -> {
                return PoolPm.getAssetToken(addressOrAsset).getString(PoolPm.OWNER)
            }
            false -> {
                return addressOrAsset
            }
        }
    }

    fun getAddressTokens(address: String): JSONArray {
        try {
            var normalizedAddress : String = address.lowercase()
            if (normalizedAddress.startsWith(STAKE_PREFIX)) {
                return getAccountAssets(normalizedAddress)
            }
            if (normalizedAddress.startsWith(HANDLE_PREFIX)) {
                normalizedAddress = lookupHandle(normalizedAddress)
            }
            val addressInfo : JSONObject = getAddressInfo(normalizedAddress)
            if (addressInfo.has(STAKE_ADDRESS_KEY)) {
                return getAccountAssets(addressInfo.getString(STAKE_ADDRESS_KEY))
            }
            return addressInfo.getJSONArray("amount")
        } catch (e : Exception) {
            // Error, return an empty array
            return JSONArray()
        }
    }

    private fun lookupHandle(handle: String) : String {
        val handleToken = getTokenNameFor(HANDLE_POLICY_ID, handle.substring(HANDLE_PREFIX.length))
        val addressesForHandle = callBlockfrostCardanoApi("assets/${handleToken}/addresses")
        return JSONArray(addressesForHandle).getJSONObject(0).getString("address")
    }

    fun getTokenNameFor(policy: String, readableName: String) : String {
        val nameAsBytes = readableName.toByteArray(StandardCharsets.UTF_8)
        return "${policy}${Hex.encodeHexString(nameAsBytes)}"
    }

    private fun getAddressInfo(address: String) : JSONObject {
        return JSONObject(callBlockfrostCardanoApi("addresses/${address}"))
    }

    private fun callBlockfrostCardanoApi(endpoint: String) : String {
        with(URL("${BLOCKFROST_CARDANO_PREFIX}/${endpoint}").openConnection() as HttpURLConnection) {
            setRequestProperty("Content-Type", BLOCKFROST_CONTENT_TYPE)
            setRequestProperty("project_id", cardanoKey)
            return String(inputStream.readBytes(), StandardCharsets.UTF_8)
        }
    }

    private fun getAccountAssets(stakeAddress: String) : JSONArray {
        return callPaginatedBlockfrostApi("accounts/${stakeAddress}/addresses/assets")
    }

    private fun callPaginatedBlockfrostApi(endpoint: String) : JSONArray {
        var page = 1
        val results = JSONArray()
        while (true) {
            val nextResult = JSONArray(callBlockfrostCardanoApi("${endpoint}?page=${page}"))
            for (index in 0 until nextResult.length()) {
                results.put(nextResult.get(index))
            }
            if (nextResult.length() < PAGE_MAX) {
                return results
            }
            page++
        }
    }

    fun getNftMediaImage(token: JSONObject): Pair<String, Any>? {
        val tokenInformation = JSONObject(callBlockfrostCardanoApi("assets/${token.getString(TOKEN_KEY)}"))
        val tokenMetadata = tokenInformation.getJSONObject("onchain_metadata")
        if (tokenMetadata.has("image")) {
            val mediaType = if (tokenMetadata.has("mediaType")) tokenMetadata.getString("mediaType") else ""
            return Pair(mediaType, tokenMetadata.get("image"))
        }
        return null
    }

    fun getDataMaybeFromIpfs(url: String) : InputStream {
        if (url.startsWith(IPFS_PROTOCOL)) {
            return retrieveFromIpfs(ipfsAware(url))
        }
        return URL(url).openConnection().getInputStream()
    }

    private fun ipfsAware(imageUrl: String): String {
        val cidV0Start : Int = imageUrl.indexOf(IPFS_V0_START)
        val cidV1Start : Int = imageUrl.indexOf(IPFS_V1_START)
        if (cidV0Start >= 0) {
            return imageUrl.substring(cidV0Start)
        } else if (cidV1Start >= 0) {
            return imageUrl.substring(cidV1Start)
        }
        return imageUrl
    }

    private fun retrieveFromIpfs(ipfsCid: String) : InputStream {
        with(URL("${BLOCKFROST_IPFS_PREFIX}/${ipfsCid}").openConnection() as HttpURLConnection) {
            setRequestProperty("Content-Type", BLOCKFROST_CONTENT_TYPE)
            setRequestProperty("project_id", ipfsKey)
            return inputStream
        }
    }

}