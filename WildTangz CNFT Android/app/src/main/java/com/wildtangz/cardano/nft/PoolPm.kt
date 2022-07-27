package com.wildtangz.cardano.nft

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.nio.charset.StandardCharsets

class PoolPm {

    companion object {

        val ASSET_PREFIX : String = "asset1"
        val OWNER : String = "owner"

        private val NAME : String = "name"
        private val POLICY : String = "policy"
        private val QUANTITY : String = "quantity"

        private val BASE_URL : String = "https://pool.pm"
        private val ASSET : String = "asset"

        @JvmStatic
        fun convertToBlockfrost(asset: String, blockfrost: Blockfrost) : JSONObject {
            val assetData = getAssetToken(asset)
            val assetName = blockfrost.getTokenNameFor(assetData.getString(POLICY), assetData.getString(NAME))
            val blockfrostJson = JSONObject()
            blockfrostJson.put(Blockfrost.TOKEN_KEY, assetName)
            blockfrostJson.put(Blockfrost.QUANTITY_KEY, assetData.get(QUANTITY))
            return blockfrostJson
        }

        @JvmStatic
        fun getAssetToken(asset: String): JSONObject {
            try {
                val assetText =
                    URL("${BASE_URL}/${ASSET}/${asset}").readText(StandardCharsets.UTF_8)
                return JSONObject(assetText)
            } catch (e : Exception) {
                // Error, return an empty array
            }
            return JSONObject()
        }
    }

}