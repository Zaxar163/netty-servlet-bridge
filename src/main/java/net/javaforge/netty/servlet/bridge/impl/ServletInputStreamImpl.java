/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.javaforge.netty.servlet.bridge.impl;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

import javax.servlet.ServletInputStream;
import java.io.IOException;

public class ServletInputStreamImpl extends ServletInputStream {

    public HttpRequest request;

    private ByteBufInputStream in;

    public ServletInputStreamImpl(FullHttpRequest request) {
        this.request = request;

        this.in = new ByteBufInputStream(request.content());
    }

    public ServletInputStreamImpl(HttpRequest request) {
        this.request = request;

        this.in = new ByteBufInputStream(Unpooled.buffer(0));
    }


    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return this.in.read(buf);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        return this.in.read(buf, offset, len);
    }

}
