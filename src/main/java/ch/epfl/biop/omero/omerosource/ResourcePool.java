package ch.epfl.biop.omero.omerosource;

/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/**
 * Copyright 2013 DB TSAI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * https://www.dbtsai.com/blog/2013/java-concurrent-dynamic-object-pool-for-non-thread-safe-objects-using-blocking-queue/
 *
 * Created with IntelliJ IDEA.
 * User: dtsai
 * Date: 2/18/13
 * Time: 3:42 PM
 */

public abstract class ResourcePool<Resource> {
    private final BlockingQueue<Resource> pool;
    private final ReentrantLock lock = new ReentrantLock();
    private int createdObjects = 0;
    private int size;

    protected ResourcePool(int size) {
        this(size, false);
    }

    protected ResourcePool(int size, Boolean dynamicCreation) {
        // Enable the fairness; otherwise, some threads may wait forever.
        pool = new ArrayBlockingQueue<>(size, true);
        this.size = size;
        if (!dynamicCreation) {
            lock.lock();
        }
    }

    public Resource acquire() throws Exception {
        if (!lock.isLocked()) {
            if (lock.tryLock()) {
                try {
                    ++createdObjects;
                    return createObject();
                } finally {
                    if (createdObjects < size) lock.unlock();
                }
            }
        }
        return pool.take();
    }

    public void recycle(Resource resource) throws Exception {
        // Will throws Exception when the queue is full,
        // but it should never happen.
        pool.add(resource);
    }

    public void createPool() {
        if (lock.isLocked()) {
            for (int i = 0; i < size; ++i) {
                pool.add(createObject());
                createdObjects++;
            }
        }
    }

    protected abstract Resource createObject();
}
