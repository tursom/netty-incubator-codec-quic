/*
 * Copyright 2023 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cn.tursom.netty.incubator.codec.quic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QuicTransportParametersTest extends AbstractQuicTest {

    @ParameterizedTest
    @MethodSource("newSslTaskExecutors")
    public void testParameters(Executor executor) throws Throwable {
        Channel server = null;
        Channel channel = null;
        Promise<QuicTransportParameters> serverParams = ImmediateEventExecutor.INSTANCE.newPromise();
        QuicChannelValidationHandler serverHandler = new QuicChannelValidationHandler() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                super.channelActive(ctx);
                QuicheQuicChannel channel = (QuicheQuicChannel) ctx.channel();
                serverParams.setSuccess(channel.peerTransportParameters());
            }
        };
        QuicChannelValidationHandler clientHandler = new QuicChannelValidationHandler();
        try {
            server = QuicTestUtils.newServer(executor, serverHandler, new ChannelInboundHandlerAdapter() {
                @Override
                public boolean isSharable() {
                    return true;
                }
            });
            channel = QuicTestUtils.newClient(executor);

            QuicChannel quicChannel = QuicTestUtils.newQuicChannelBootstrap(channel)
                    .handler(clientHandler)
                    .streamHandler(new ChannelInboundHandlerAdapter())
                    .remoteAddress(server.localAddress())
                    .connect().get();
            assertTransportParameters(quicChannel.peerTransportParameters());
            assertTransportParameters(serverParams.sync().getNow());

            quicChannel.close().sync();
            serverHandler.assertState();
            clientHandler.assertState();
        } finally {
            QuicTestUtils.closeIfNotNull(channel);
            QuicTestUtils.closeIfNotNull(server);

            shutdown(executor);
        }
    }

    private static void assertTransportParameters(@Nullable QuicTransportParameters parameters) {
        assertNotNull(parameters);
        assertThat(parameters.maxIdleTimeout()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.maxUdpPayloadSize()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxData()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxStreamDataBidiLocal()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxStreamDataBidiRemote()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxStreamDataUni()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxStreamsBidi()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.initialMaxStreamsUni()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.ackDelayExponent()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.maxAckDelay()).isGreaterThanOrEqualTo(1L);
        assertFalse(parameters.disableActiveMigration());
        assertThat(parameters.activeConnIdLimit()).isGreaterThanOrEqualTo(1L);
        assertThat(parameters.maxDatagramFrameSize()).isGreaterThanOrEqualTo(0L);
    }
}
