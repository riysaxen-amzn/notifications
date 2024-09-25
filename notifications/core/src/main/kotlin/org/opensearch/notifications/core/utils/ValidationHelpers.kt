/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.notifications.core.utils

import inet.ipaddr.HostName
import inet.ipaddr.IPAddressString
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.logging.log4j.LogManager
import org.opensearch.core.common.Strings
import java.lang.Exception
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

fun validateUrl(urlString: String) {
    require(!Strings.isNullOrEmpty(urlString)) { "url is null or empty" }
    require(isValidUrl(urlString)) { "Invalid URL or unsupported" }
}

fun validateUrlHost(urlString: String, hostDenyList: List<String>) {
    val url = URL(urlString)

    if (org.opensearch.notifications.spi.utils.getResolvedIps(url.host).isEmpty()) {
        throw UnknownHostException("Host could not be resolved to a valid Ip address")
    }

    require(!org.opensearch.notifications.spi.utils.isHostInDenylist(urlString, hostDenyList)) {
        "Host of url is denied, based on plugin setting [notification.core.http.host_deny_list]"
    }
}

fun validateEmail(email: String) {
    require(!Strings.isNullOrEmpty(email)) { "FromAddress and recipient should be provided" }
    require(isValidEmail(email)) { "Invalid email address" }
}

fun isValidUrl(urlString: String): Boolean {
    val url = URL(urlString) // throws MalformedURLException if URL is invalid
    return ("https" == url.protocol || "http" == url.protocol) // Support only http/https, other protocols not supported
}

@Deprecated("This function is not maintained, use org.opensearch.notifications.spi.utils.isHostInDenylist instead.")
fun isHostInDenylist(urlString: String, hostDenyList: List<String>): Boolean {
    val url = URL(urlString)
    if (url.host != null) {
        try {
            val resolvedIps = InetAddress.getAllByName(url.host)
            val resolvedIpStrings = resolvedIps.map { inetAddress -> IPAddressString(inetAddress.hostAddress) }
            val hostStr = HostName(url.host)

            for (network in hostDenyList) {
                val denyIpStr = IPAddressString(network)
                val denyHostStr = HostName(network)
                val hostInDenyList = denyHostStr.equals(hostStr)
                var ipInDenyList = false

                for (ipStr in resolvedIpStrings) {
                    if (denyIpStr.contains(ipStr)) {
                        ipInDenyList = true
                        break
                    }
                }

                if (hostInDenyList || ipInDenyList) {
                    LogManager.getLogger().error("${url.host} is denied")
                    return true
                }
            }
        } catch (e: UnknownHostException) {
            LogManager.getLogger().error("Error checking denylist: Unknown host")
            return false
        } catch (e: Exception) {
            LogManager.getLogger().error("Error checking denylist: ${e.message}", e)
            return false
        }
    }

    return false
}

/**
 * RFC 5322 compliant pattern matching: https://www.ietf.org/rfc/rfc5322.txt
 * Regex was based off of this post: https://stackoverflow.com/a/201378
 */
fun isValidEmail(email: String): Boolean {
    val validEmailPattern = Regex(
        "(?:[a-z0-9!#\$%&'*+\\/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+\\/=?^_`{|}~-]+)*" +
            "|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]" + "" +
            "|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
            "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" +
            "|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}" +
            "(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:" +
            "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]" + "" +
            "|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])",
        RegexOption.IGNORE_CASE
    )
    return validEmailPattern.matches(email)
}

fun validateMethod(method: String) {
    require(!Strings.isNullOrEmpty(method)) { "Method is null or empty" }
    val validMethods = listOf(HttpPost.METHOD_NAME, HttpPut.METHOD_NAME, HttpPatch.METHOD_NAME)
    require(
        method.findAnyOf(validMethods) != null
    ) { "Invalid method supplied. Only POST, PUT and PATCH are allowed" }
}
