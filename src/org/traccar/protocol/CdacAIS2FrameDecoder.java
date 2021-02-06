/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;
import java.nio.charset.StandardCharsets;

public class CdacAIS2FrameDecoder extends BaseFrameDecoder {

    public CdacAIS2FrameDecoder() { }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 4) {
            return null;
        }

        String header = String.valueOf(buf.getCharSequence(buf.readerIndex(), 3, StandardCharsets.US_ASCII));
        switch (header) {
            case "NRM":
            case "EPB":
            case "CRT":
            case "ALT":
                if (buf.readableBytes() >= 96 + 3) {
                    return buf.readRetainedSlice(96 + 3);
                }
                break;
            case "FUL":
                if (buf.readableBytes() >= 225 + 3) {
                    return buf.readRetainedSlice(225 + 3);
                }
                break;
            case "BTH":
                int batchLogCount = Integer.parseInt(
                        String.valueOf(
                                buf.getCharSequence(buf.readerIndex() + 18, 3, StandardCharsets.US_ASCII)
                        )
                );

                if (buf.readableBytes() >= (batchLogCount * 78) + 3 + 15 + 3) {
                    return buf.readRetainedSlice((batchLogCount * 78) + 3 + 15 + 3);
                }
                break;
            case "LGN":
            case "HBT":
                if (buf.readableBytes() >= 15 + 3) {
                    return buf.readRetainedSlice(15 + 3);
                }
                break;
            case "HLM":
                if (buf.readableBytes() >= 59 + 3) {
                    return buf.readRetainedSlice(59 + 3);
                }
                break;
            case "ACK":
                int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) '*');
                if (index >= 0) {
                    return buf.readRetainedSlice(index + 1);
                }
                break;
            default:
                break;
        }

        return null;
    }

}
