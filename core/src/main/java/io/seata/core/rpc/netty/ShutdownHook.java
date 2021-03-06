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
package io.seata.core.rpc.netty;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import io.seata.common.util.CollectionUtils;
import io.seata.core.rpc.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ensure the shutdownHook is singleton
 * TODO: 定义的一个关闭钩子 注释的是要保证单例
 *
 * @author 563868273@qq.com
 */
public class ShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);


    /**
     * 直接new 一个钩子，private 的，饿汉单例模式
     */
    private static final ShutdownHook SHUTDOWN_HOOK = new ShutdownHook("ShutdownHook");

    /**
     * 需要进行关闭的一个集合
     */
    private Set<Disposable> disposables = new TreeSet<>();

    /**
     * 原子操作类
     */
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * default 10. Lower values have higher priority
     * 默认的优先级
     */
    private static final int DEFAULT_PRIORITY = 10;

    /**
     * 静态，在关闭Jvm的时候 会回调钩子
     */
    static {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
    }

    private ShutdownHook(String name) {
        super(name);
    }

    public static ShutdownHook getInstance() {
        return SHUTDOWN_HOOK;
    }

    /**
     * 添加一个要关闭的东东
     * @param disposable
     */
    public void addDisposable(Disposable disposable) {
        addDisposable(disposable, DEFAULT_PRIORITY);
    }

    /**
     * 添加一个可以指定优先级的钩子
     * @param disposable
     * @param priority
     */
    public void addDisposable(Disposable disposable, int priority) {
        // TODO: 又进行包了一层，可以进行优先级比较
        disposables.add(new DisposablePriorityWrapper(disposable, priority));
    }

    /**
     * 开始调用关闭方法呗
     */
    @Override
    public void run() {
        destroyAll();
    }


    /**
     * 关闭注册的所有的东东
     */
    public void destroyAll() {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destoryAll starting");
        }
        // TODO: 这地方有意思，需要保证原子性判断，然后如果为空的话，直接给return掉就好了呗
        if (!destroyed.compareAndSet(false, true) && CollectionUtils.isEmpty(disposables)) {
            return;
        }
        // TODO: for循环destroy
        for (Disposable disposable : disposables) {
            disposable.destroy();
        }
    }

    /**
     * for spring context
     * 移除钩子
     */
    public static void removeRuntimeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(SHUTDOWN_HOOK);
    }

    private static class DisposablePriorityWrapper implements Comparable<DisposablePriorityWrapper>, Disposable {

        private Disposable disposable;

        private int priority;

        public DisposablePriorityWrapper(Disposable disposable, int priority) {
            this.disposable = disposable;
            this.priority = priority;
        }

        @Override
        public int compareTo(DisposablePriorityWrapper disposablePriorityWrapper) {
            return priority - disposablePriorityWrapper.priority;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.priority);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DisposablePriorityWrapper)) {
                return false;
            }
            DisposablePriorityWrapper dpw = (DisposablePriorityWrapper)other;
            return this.priority == dpw.priority && this.disposable.equals(dpw.disposable);
        }

        @Override
        public void destroy() {
            disposable.destroy();
        }
    }

}

