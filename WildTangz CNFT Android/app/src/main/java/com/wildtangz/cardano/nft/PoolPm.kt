package com.wildtangz.cardano.nft

import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.nio.charset.Charset

class PoolPm {

    companion object {

        private val BASE_URL : String = "https://pool.pm"
        private val ASSET : String = "asset"
        private val OWNER : String = "owner"
        private val POLICY : String = "policy"
        private val WALLET : String = "wallet"
        private val CHARSET : String = "UTF-8"

        private val ASSET_PREFIX : String = "asset"

        private val IPFS_GATEWAY : String = "https://infura-ipfs.io/ipfs"
        private val IPFS_PROTOCOL : String = "ipfs://"
        private val IPFS_V1_START : String = "Q"
        private val IPFS_V2 : String = "bafy"

        @JvmStatic
        fun hasAmountPolicyTokens(assetOrAddress: String, policy: String, amount: Int) : Boolean {
            val ownedTokens = getAddressTokens(getWalletName(assetOrAddress))
            var numFound = 0
            for (index in 0 until ownedTokens.length()) {
                if (ownedTokens.getJSONObject(index).getString(POLICY) == policy) {
                    numFound++
                }
            }
            return numFound >= amount
        }

        private fun getWalletName(addressOrAsset: String): String {
            when (addressOrAsset.startsWith(ASSET_PREFIX)) {
                true -> {
                    val assetToken = getAssetToken(addressOrAsset)
                    return assetToken.getJSONObject(0).getString(OWNER)
                }
                false -> {
                    return addressOrAsset
                }
            }
        }

        @JvmStatic
        fun getAddressTokens(address: String): JSONArray {
            try {
                val userWalletText =
                    URL("${BASE_URL}/${WALLET}/${address}").readText(Charset.forName(CHARSET))
                val userWallet = JSONObject(userWalletText)
                return userWallet.getJSONArray("tokens")
            } catch (e : Exception) {
                // Error, return an empty array
                return JSONArray()
            }
        }

        @JvmStatic
        fun getAssetToken(asset: String): JSONArray {
            val jsonArray = JSONArray()
            try {
                val assetText =
                    URL("${BASE_URL}/${ASSET}/${asset}").readText(Charset.forName(CHARSET))
                jsonArray.put(JSONObject(assetText))
            } catch (e : Exception) {
                // Error, return an empty array
            }
            return jsonArray
        }

        @JvmStatic
        fun getNftUrls(addressOrAsset: String): List<Pair<String, Any>> {
            val tokens = when (addressOrAsset.startsWith(ASSET_PREFIX)) {
                true -> getAssetToken(addressOrAsset)
                false -> getAddressTokens(addressOrAsset)
            }
            return getAllNftImageUrls(tokens)
        }

        private fun getAllNftImageUrls(tokens: JSONArray): List<Pair<String, Any>> {
            val eligibleImages = mutableListOf<Pair<String, Any>>()
            for (index in 0 until tokens.length()) {
                val token = tokens.getJSONObject(index)
                if (token.getInt("quantity") == 1 && token.has("metadata")) {
                    val tokenMetadata = token.getJSONObject("metadata")
                    if (tokenMetadata.has("image")) {
                        val mediaType = if (tokenMetadata.has("mediaType")) tokenMetadata.getString("mediaType") else ""
                        eligibleImages.add(Pair(mediaType, tokenMetadata.get("image")))
                    }
                }
            }
            return eligibleImages
        }

        @JvmStatic
        fun convertedToWeb(imageUrl: String): String {
            if (imageUrl.startsWith(IPFS_PROTOCOL)) {
                val cidv0 = imageUrl.substring(imageUrl.indexOf(IPFS_V1_START))
                return "${IPFS_GATEWAY}/${cidv0}"
            }

            if (imageUrl.startsWith(IPFS_V2)) {
                return "${IPFS_GATEWAY}/${imageUrl}"
            }

            return imageUrl
        }
    }

}