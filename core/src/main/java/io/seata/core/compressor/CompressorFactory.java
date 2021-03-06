/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.compressor;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the type compressor factory
 * 压缩工厂
 * @author jsbxyyx
 */
public class CompressorFactory {

    /**
     * The constant COMPRESSOR_MAP.
     */
    protected static final Map<CompressorType, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<CompressorType, Compressor>();

    static {
        // TODO: 默认存了一个 不压缩的
        COMPRESSOR_MAP.put(CompressorType.NONE, new NoneCompressor());
    }

    /**
     * Get compressor by code.
     *
     * @param code the code
     * @return the compressor
     */
    public static Compressor getCompressor(byte code) {
        // TODO: 根据code拿到一个压缩器
        CompressorType type = CompressorType.getByCode(code);
        // TODO: 如果本地缓存有，直接返回
        if (COMPRESSOR_MAP.get(type) != null) {
            return COMPRESSOR_MAP.get(type);
        }
        // TODO: 否则去全局加载去，然后加到本类缓存中，之后返回
        Compressor impl = EnhancedServiceLoader.load(Compressor.class, type.name());
        COMPRESSOR_MAP.putIfAbsent(type, impl);
        return impl;
    }

    /**
     * None compressor
     */
    @LoadLevel(name = "NONE")
    public static class NoneCompressor implements Compressor {
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes;
        }

        @Override
        public byte[] decompress(byte[] bytes) {
            return bytes;
        }
    }

}
