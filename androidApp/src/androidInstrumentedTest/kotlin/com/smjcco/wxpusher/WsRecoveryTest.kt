package com.smjcco.wxpusher

import android.app.Application
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smjcco.wxpusher.app.WxpBaseInfoServiceImpl
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.push.ws.connect.*
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.WebSocket
import okio.ByteString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class WsRecoveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val server = MockWebServer()
    private val serverSockets = java.util.concurrent.ConcurrentLinkedQueue<WebSocket>()
    private val old = FakeSocket()
    private val fresh = FakeSocket()
    private fun field(name: String) = WsManager::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun put(name: String, value: Any?) { field(name).set(WsManager, value) }
    @Suppress("UNCHECKED_CAST")
    private fun listeners() = field("msgListenerMap").get(WsManager) as MutableMap<Int, MutableList<IWsMessageListener<out BaseWsMsg>>>
    @Before fun setup() {
        val app = instrumentation.targetContext.applicationContext as Application
        ApplicationUtils.init(app)
        WxpSaveService.init()
        WxpBaseInfoService.init(WxpBaseInfoServiceImpl())
        WxpConfig.baseUrl = "http://wxpusher.test.zjiecode.com"
        WxpConfig.appFeUrl = "http://wxpusher.test.zjiecode.com"
        server.start()
        WxpConfig.wsUrl = server.url("/").toString()
        repeat(3) {
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, reason) }
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSockets.add(webSocket)
                    webSocket.send("{\"msgType\":202,\"pushToken\":\"PT_test_only\"}")
                    webSocket.send("{\"msgType\":20001,\"mid\":4242}")
                }
            }))
        }
        instrumentation.runOnMainSync { WsManager.init(); WsManager.stop(); listeners().clear() }
        put("enabled", true)
        put("disableConnect", false)
        put("webSocket", old)
        @Suppress("UNCHECKED_CAST")
        (field("connectStatus").get(WsManager) as AtomicReference<WsManager.WsConnectStatus>).set(WsManager.WsConnectStatus.Connected)
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync {
            (field("webSocket").get(WsManager) as? WebSocket)?.cancel()
            WsManager.stop()
            listeners().clear()
        }
        serverSockets.forEach { it.close(1000, "test complete") }
        server.shutdown()
    }

    @Test fun lateMessagesCannotResurrectStoppedChannel() {
        WsManager.stop()
        WsManager.WsListener().onMessage(old, "{\"msgType\":202,\"pushToken\":\"old\"}")
        WsManager.WsListener().onMessage(old, ByteString.EMPTY)
        assertEquals(WsManager.WsConnectStatus.NotConnect, WsManager.getConnectStatus())
        assertNull(field("webSocket").get(WsManager))
        WsManager.tryConnect("foreground")
        WsManager.tryConnect("network-unblocked")
        assertNull(field("webSocket").get(WsManager))
    }

    @Test fun queuedOldTokenCannotOverwriteNewConnection() {
        var delivered = 0
        WsManager.addMsgListener(202, object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) { delivered++ }
        })
        instrumentation.runOnMainSync {
            WsManager.WsListener().onMessage(old, "{\"msgType\":202,\"pushToken\":\"old\"}")
            put("webSocket", fresh)
        }
        instrumentation.waitForIdleSync()
        assertEquals(0, delivered)
        WsManager.WsListener().onMessage(old, "{\"msgType\":204}")
        assertSame(fresh, field("webSocket").get(WsManager))
    }

    @Test fun acceptedBusinessMessageSurvivesChannelSwitch() {
        val delivered = CountDownLatch(1)
        WsManager.addMsgListener(20001, object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) { assertEquals(42L, message.mid); delivered.countDown() }
        })
        instrumentation.runOnMainSync {
            WsManager.WsListener().onMessage(old, "{\"msgType\":20001,\"mid\":42}")
            WsManager.stop()
        }
        assertTrue(delivered.await(3, TimeUnit.SECONDS))
        assertEquals(WsManager.WsConnectStatus.NotConnect, WsManager.getConnectStatus())
    }

    @Test fun canceledCallbackCannotConsumeNewRetry() {
        val due = SystemClock.elapsedRealtime() + 120000
        put("reconnectDueElapsed", due)
        put("reconnectDueUptime", SystemClock.uptimeMillis() + 120000)
        put("reconnectMode", "handler")
        WsManager::class.java.getDeclaredMethod("runScheduledReconnect").apply { isAccessible = true }.invoke(WsManager)
        assertEquals(due, field("reconnectDueElapsed").getLong(WsManager))
        assertSame(old, field("webSocket").get(WsManager))
    }

    @Test fun localTestServerReceivesMessagesAndRecoversAfterSocketFailure() {
        val init = CountDownLatch(2)
        val messages = CountDownLatch(2)
        WsManager.addMsgListener(20001, object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) { assertEquals(4242L, message.mid); messages.countDown() }
        })
        WsManager.addMsgListener(202, object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) { assertTrue(message.pushToken.startsWith("PT_")); init.countDown() }
        })
        WsManager.stop()
        WsManager.start()
        val deadline = SystemClock.elapsedRealtime() + 20000
        while (init.count == 2L && SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
        assertEquals("local server must deliver DEVICE_INIT", 1L, init.count)
        (field("webSocket").get(WsManager) as WebSocket).cancel()
        assertTrue("existing retry must reconnect to local server", init.await(25, TimeUnit.SECONDS))
        assertTrue("messages delivered before and after recovery", messages.await(3, TimeUnit.SECONDS))
        assertEquals(WsManager.WsConnectStatus.Connected, WsManager.getConnectStatus())
    }

    @Test fun networkRecoveryDoesNotWaitForThirtySecondOfflinePoll() {
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        } }
        val received = CountDownLatch(1)
        WsManager.addMsgListener(202, object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) { received.countDown() }
        })
        try {
            WsManager.stop()
            shell("svc wifi disable")
            shell("svc data disable")
            Thread.sleep(1500)
            WsManager.start()
            assertTrue(field("networkUnavailable").getBoolean(WsManager))
            shell("svc wifi enable")
            shell("svc data enable")
            assertTrue("network callback must recover before the 30s poll", received.await(20, TimeUnit.SECONDS))
        } finally {
            shell("svc wifi enable")
            shell("svc data enable")
        }
    }

    @Test fun capabilityChangesRespectBackoffAndStoppedChannel() {
        val network = (instrumentation.targetContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager).activeNetwork!!
        val callback = WsManager.RecoveryNetworkCallback()
        callback.onAvailable(network) // current fake socket is already connected
        put("webSocket", null)
        @Suppress("UNCHECKED_CAST")
        (field("connectStatus").get(WsManager) as AtomicReference<WsManager.WsConnectStatus>).set(WsManager.WsConnectStatus.NotConnect)
        val due = SystemClock.elapsedRealtime() + 120000
        put("reconnectDueElapsed", due)
        val attempts = field("attempts").getInt(WsManager)
        repeat(10) {
            val cm = instrumentation.targetContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val caps = if (it % 2 == 0) cm.getNetworkCapabilities(network)!! else android.net.NetworkCapabilities()
            callback.onCapabilitiesChanged(network, caps)
            callback.onBlockedStatusChanged(network, false)
        }
        assertEquals(attempts, field("attempts").getInt(WsManager))
        assertEquals(due, field("reconnectDueElapsed").getLong(WsManager))
        WsManager.stop()
        callback.onBlockedStatusChanged(network, true)
        callback.onBlockedStatusChanged(network, false)
        callback.onCapabilitiesChanged(network, android.net.NetworkCapabilities())
        assertNull(field("webSocket").get(WsManager))
    }

    private fun markDisconnected() {
        put("webSocket", null)
        @Suppress("UNCHECKED_CAST")
        (field("connectStatus").get(WsManager) as AtomicReference<WsManager.WsConnectStatus>).set(WsManager.WsConnectStatus.NotConnect)
    }

    @Test fun capabilityRecoveryAndInitialUnblockedOnlyConnectOnce() {
        val cm = instrumentation.targetContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork!!
        val callback = WsManager.RecoveryNetworkCallback()
        callback.onAvailable(network)
        markDisconnected()
        put("networkUnavailable", true)
        val attempts = field("attempts").getInt(WsManager)
        callback.onCapabilitiesChanged(network, cm.getNetworkCapabilities(network)!!)
        callback.onBlockedStatusChanged(network, false)
        callback.onCapabilitiesChanged(network, cm.getNetworkCapabilities(network)!!)
        assertEquals(attempts + 1, field("attempts").getInt(WsManager))
    }

    @Test fun blockedReleaseOnlyConnectsOnce() {
        val cm = instrumentation.targetContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork!!
        val callback = WsManager.RecoveryNetworkCallback()
        callback.onAvailable(network)
        callback.onBlockedStatusChanged(network, true)
        markDisconnected()
        val attempts = field("attempts").getInt(WsManager)
        callback.onBlockedStatusChanged(network, false)
        callback.onBlockedStatusChanged(network, false)
        callback.onCapabilitiesChanged(network, cm.getNetworkCapabilities(network)!!)
        assertEquals(attempts + 1, field("attempts").getInt(WsManager))
    }

    @Test fun foregroundRecoveryIsImmediateAndPageChangesAreMerged() {
        val diagnostics = com.smjcco.wxpusher.push.ws.WsDiagnostics
        fun diagPut(name: String, value: Any) { diagnostics.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(diagnostics, value) }
        markDisconnected()
        put("reconnectDueElapsed", SystemClock.elapsedRealtime() + 120000)
        val attempts = field("attempts").getInt(WsManager)
        try {
            diagPut("initialized", true)
            diagPut("foreground", false)
            diagPut("startedActivities", 0)
            instrumentation.runOnMainSync {
                diagnostics.onActivityStarted(android.app.Activity())
                diagnostics.onActivityStarted(android.app.Activity())
            }
            assertEquals(attempts + 1, field("attempts").getInt(WsManager))
        } finally {
            diagPut("initialized", false)
            diagPut("foreground", false)
            diagPut("startedActivities", 0)
        }
    }

    @Test fun acceptedMessagePostsOneNotificationForRepeatedMid() {
        val manager = com.smjcco.wxpusher.push.ws.WxpNotificationManager
        val alerts = com.smjcco.wxpusher.push.ws.alert.WsAlertStore
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        val notifications = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val title = "WS recovery test " + System.currentTimeMillis()
        val duration = alerts.getDurationSeconds()
        try {
            alerts.setDurationSeconds(0)
            manager.init()
            WsManager.addMsgListener(20001, object : IWsMessageListener<PushMsgDeviceMsg> {
                override fun onMessage(message: PushMsgDeviceMsg) { manager.sendBizMessageNotification(message) }
            })
            val json = "{\"msgType\":20001,\"mid\":${System.currentTimeMillis()},\"title\":\"$title\",\"summary\":\"test\",\"url\":\"http://localhost/\"}"
            WsManager.WsListener().onMessage(old, json)
            WsManager.WsListener().onMessage(old, json)
            instrumentation.waitForIdleSync()
            assertEquals(1, notifications.activeNotifications.count { it.notification.extras.getCharSequence("android.title") == title })
        } finally {
            notifications.activeNotifications.filter { it.notification.extras.getCharSequence("android.title") == title }.forEach { notifications.cancel(it.id) }
            alerts.setDurationSeconds(duration)
        }
    }

    private class FakeSocket : WebSocket {
        override fun request() = Request.Builder().url("http://localhost/").build()
        override fun queueSize() = 0L
        override fun send(text: String) = true
        override fun send(bytes: ByteString) = true
        override fun close(code: Int, reason: String?) = true
        override fun cancel() {}
    }
}
