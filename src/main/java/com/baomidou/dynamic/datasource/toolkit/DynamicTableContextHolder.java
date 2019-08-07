/**
 * Copyright © 2018 organization baomidou
 * <pre>
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
 * <pre/>
 */
package com.baomidou.dynamic.datasource.toolkit;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核心基于ThreadLocal的切换数据源工具类
 *
 * @author renxl
 * @since 1.0.0
 */

public final class DynamicTableContextHolder {
    // 如果threadlocal.get之后的副本，只在当前线程中使用，那么是线程安全的；如果对其他线程暴露，不一定是线程安全的。
    // 切记list的引用不可暴漏出去！！！！！！！！！！！！！

    public static final ThreadLocal<List<String>> tablesInfo = new ThreadLocal();

    public static final ThreadLocal<AtomicInteger> aopNum = new ThreadLocal();




    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("1",12);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(new byte[12]);
        ServerSocket sc = new ServerSocket(1223);
        Socket accept = sc.accept(); // 阻塞
        accept.getOutputStream().write(new byte[11]);// BIO阻塞



    }

}
