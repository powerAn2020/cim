package com.crossoverjie.cim.client.sdk;

import com.crossoverjie.cim.client.sdk.route.AbstractRouteBaseTest;
import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import com.crossoverjie.cim.route.api.vo.req.P2PReqVO;
import com.crossoverjie.cim.route.api.vo.res.CIMServerResVO;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class ClientTest extends AbstractRouteBaseTest {


    @AfterEach
    public void tearDown() {
        super.close();
    }

    @Test
    public void groupChat() throws Exception {
        super.starSingleServer();
        super.startRoute();
        String routeUrl = "http://localhost:8083";
        String c1 = "crossoverJie";
        String zs = "zs";
        Long id = super.registerAccount(c1);
        Long zsId = super.registerAccount(zs);

        @Cleanup
        Client client1 = Client.builder()
                .userName(c1)
                .userId(id)
                .routeUrl(routeUrl)
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state = client1.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state));
        Optional<CIMServerResVO> serverInfo = client1.getServerInfo();
        Assertions.assertTrue(serverInfo.isPresent());
        System.out.println("client1 serverInfo = " + serverInfo.get());


        AtomicReference<String> client2Receive = new AtomicReference<>();
        @Cleanup
        Client client2 = Client.builder()
                .userName(zs)
                .userId(zsId)
                .routeUrl(routeUrl)
                .messageListener((client, message) -> client2Receive.set(message))
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state2 = client2.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state2));

        Optional<CIMServerResVO> serverInfo2 = client2.getServerInfo();
        Assertions.assertTrue(serverInfo2.isPresent());
        System.out.println("client2 serverInfo = " + serverInfo2.get());

        // send msg
        String msg = "hello";
        client1.sendGroup(msg);

        Set<CIMUserInfo> onlineUser = client1.getOnlineUser();
        Assertions.assertEquals(onlineUser.size(), 2);
        onlineUser.forEach(userInfo -> {
            log.info("online user = {}", userInfo);
            Long userId = userInfo.getUserId();
            if (userId.equals(id)) {
                Assertions.assertEquals(c1, userInfo.getUserName());
            } else if (userId.equals(zsId)) {
                Assertions.assertEquals(zs, userInfo.getUserName());
            }
        });

        Awaitility.await().untilAsserted(
                () -> Assertions.assertEquals(String.format("crossoverJie:%s", msg), client2Receive.get()));
        ;
        super.stopSingle();
    }

    @Test
    public void testP2PChat() throws Exception {
        super.starSingleServer();
        super.startRoute();
        String routeUrl = "http://localhost:8083";
        String cj = "cj";
        String zs = "zs";
        String ls = "ls";
        Long cjId = super.registerAccount(cj);
        Long zsId = super.registerAccount(zs);
        Long lsId = super.registerAccount(ls);

        @Cleanup
        Client client1 = Client.builder()
                .userName(cj)
                .userId(cjId)
                .routeUrl(routeUrl)
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state = client1.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state));
        Optional<CIMServerResVO> serverInfo = client1.getServerInfo();
        Assertions.assertTrue(serverInfo.isPresent());
        System.out.println("client1 serverInfo = " + serverInfo.get());


        // client2
        AtomicReference<String> client2Receive = new AtomicReference<>();
        @Cleanup
        Client client2 = Client.builder()
                .userName(zs)
                .userId(zsId)
                .routeUrl(routeUrl)
                .messageListener((client, message) -> client2Receive.set(message))
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state2 = client2.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state2));

        Optional<CIMServerResVO> serverInfo2 = client2.getServerInfo();
        Assertions.assertTrue(serverInfo2.isPresent());
        System.out.println("client2 serverInfo = " + serverInfo2.get());

        // client3
        AtomicReference<String> client3Receive = new AtomicReference<>();
        @Cleanup
        Client client3 = Client.builder()
                .userName(ls)
                .userId(lsId)
                .routeUrl(routeUrl)
                .messageListener((client, message) -> {
                    log.info("client3 receive message = {}", message);
                    client3Receive.set(message);
                })
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state3 = client3.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state3));

        Optional<CIMServerResVO> serverInfo3 = client3.getServerInfo();
        Assertions.assertTrue(serverInfo3.isPresent());
        System.out.println("client3 serverInfo = " + serverInfo3.get());

        // send msg to client3
        String msg = "hello";
        client1.sendP2P(P2PReqVO.builder()
                .receiveUserId(lsId)
                .msg(msg)
                .build());

        Set<CIMUserInfo> onlineUser = client1.getOnlineUser();
        Assertions.assertEquals(onlineUser.size(), 3);
        onlineUser.forEach(userInfo -> {
            log.info("online user = {}", userInfo);
            Long userId = userInfo.getUserId();
            if (userId.equals(cjId)) {
                Assertions.assertEquals(cj, userInfo.getUserName());
            } else if (userId.equals(zsId)) {
                Assertions.assertEquals(zs, userInfo.getUserName());
            } else if (userId.equals(lsId)) {
                Assertions.assertEquals(ls, userInfo.getUserName());
            }
        });

        Awaitility.await().untilAsserted(
                () -> Assertions.assertEquals(String.format("%s:%s",cj, msg), client3Receive.get()));
        Awaitility.await().untilAsserted(
                () -> Assertions.assertNull(client2Receive.get()));
        super.stopSingle();
    }

    /**
     * 1. Start two servers.
     * 2. Start two client, and send message.
     * 3. Stop one server which is connected by client1.
     * 4. Wait for client1 reconnect.
     * 5. Send message again.
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        super.startTwoServer();
        super.startRoute();

        String routeUrl = "http://localhost:8083";
        String c1 = "cj";
        String zs = "zs";
        Long id = super.registerAccount(c1);
        Long zsId = super.registerAccount(zs);

        @Cleanup
        Client client1 = Client.builder()
                .userName(c1)
                .userId(id)
                .routeUrl(routeUrl)
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state = client1.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state));


        AtomicReference<String> client2Receive = new AtomicReference<>();
        @Cleanup
        Client client2 = Client.builder()
                .userName(zs)
                .userId(zsId)
                .routeUrl(routeUrl)
                .messageListener((client, message) -> client2Receive.set(message))
                .build();
        TimeUnit.SECONDS.sleep(3);
        ClientState.State state2 = client2.getState();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state2));

        Optional<CIMServerResVO> serverInfo2 = client2.getServerInfo();
        Assertions.assertTrue(serverInfo2.isPresent());
        System.out.println("client2 serverInfo = " + serverInfo2.get());

        // send msg
        String msg = "hello";
        client1.sendGroup(msg);
        Awaitility.await()
                .untilAsserted(() -> Assertions.assertEquals(String.format("cj:%s", msg), client2Receive.get()));
        client2Receive.set("");


        System.out.println("ready to restart server");
        TimeUnit.SECONDS.sleep(3);
        Optional<CIMServerResVO> serverInfo = client1.getServerInfo();
        Assertions.assertTrue(serverInfo.isPresent());
        System.out.println("server info = " + serverInfo.get());

        super.stopServer(serverInfo.get().getCimServerPort());
        System.out.println("stop server success! " + serverInfo.get());


        // Waiting server stopped, and client reconnect.
        TimeUnit.SECONDS.sleep(30);
        System.out.println("reconnect state: " + client1.getState());
        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(ClientState.State.Ready, state));
        serverInfo = client1.getServerInfo();
        Assertions.assertTrue(serverInfo.isPresent());
        System.out.println("client1 reconnect server info = " + serverInfo.get());

        // Send message again.
        log.info("send message again, client2Receive = {}", client2Receive.get());
        client1.sendGroup(msg);
        Awaitility.await()
                .untilAsserted(() -> Assertions.assertEquals(String.format("cj:%s", msg), client2Receive.get()));
        ;
        super.stopTwoServer();
    }

}